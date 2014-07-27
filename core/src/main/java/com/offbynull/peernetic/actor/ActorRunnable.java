package com.offbynull.peernetic.actor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ActorRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ActorRunnable.class);

    private final UnmodifiableList<Actor> actors;
    private final UnmodifiableMap<Actor, Endpoint> endpoints;
    private final LinkedBlockingQueue<InternalEnvelope> queue;
    private final AtomicReference<State> state;
    private final Lock lock;
    private final Condition startingCondition;
    private final Condition startedCondition;
    private final Condition stoppedCondition;
    private final Condition stoppingCondition;
    private volatile Thread thread;

    public static ActorRunnable createAndStart(Actor... actors) {
        ActorRunnable actorRunnable = new ActorRunnable(actors);

        actorRunnable.lock.lock();
        try {
            Thread actorThread = new Thread(actorRunnable);
            actorThread.setDaemon(true);
            actorThread.setName(ActorRunnable.class.getSimpleName() + "-Thread");
            actorThread.start();

            actorRunnable.awaitState(State.STARTING);
        } finally {
            actorRunnable.lock.unlock();
        }

        LOG.info("Created and started Actor thread with the following Actors: {}", Arrays.asList(actors));

        return actorRunnable;
    }

    public ActorRunnable(Actor... actors) {
        Validate.noNullElements(actors);
        Validate.isTrue(actors.length > 0);

        this.actors = (UnmodifiableList<Actor>) UnmodifiableList.<Actor>unmodifiableList(new ArrayList<>(Arrays.asList(actors)));
        this.queue = new LinkedBlockingQueue<>();

        Map<Actor, Endpoint> endpoints = new HashMap<>();
        for (int i = 0; i < actors.length; i++) {
            Endpoint existing = endpoints.put(actors[i], new InternalEndpoint(i, queue));
            Validate.isTrue(existing == null, "Duplicate actor");
        }

        this.endpoints = (UnmodifiableMap<Actor, Endpoint>) UnmodifiableMap.<Actor, Endpoint>unmodifiableMap(endpoints);

        this.state = new AtomicReference<>(State.CREATED);
        this.lock = new ReentrantLock();
        this.startingCondition = lock.newCondition();
        this.startedCondition = lock.newCondition();
        this.stoppedCondition = lock.newCondition();
        this.stoppingCondition = lock.newCondition();
    }

    public void awaitState(State state) {
        Validate.notNull(state);

        if (this.state.get().ordinal() >= state.ordinal()) {
            return;
        }

        lock.lock();
        try {
            if (this.state.get().ordinal() >= state.ordinal()) {
                return;
            }

            switch (state) {
                case CREATED:
                    break;
                case STARTING:
                    startingCondition.awaitUninterruptibly();
                    break;
                case STARTED:
                    startedCondition.awaitUninterruptibly();
                    break;
                case STOPPING:
                    stoppingCondition.awaitUninterruptibly();
                    break;
                case STOPPED:
                    stoppedCondition.awaitUninterruptibly();
                    break;
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    public Endpoint getEndpoint(Actor actor) {
        Validate.notNull(actor);

        Endpoint endpoint = endpoints.get(actor);
        Validate.isTrue(endpoint != null, "Actor not found");

        return endpoint;
    }

    public Thread getThread() {
        Validate.validState(this.state.get() != State.CREATED); // must be >= STARTING for thread to be non-null

        return thread;
    }

    public void shutdown() throws InterruptedException {
        Validate.validState(this.state.get() != State.CREATED); // must be >= STARTING for thread to be running

        thread.interrupt();
        thread.join();
    }

    @Override
    public void run() {
        Validate.validState(thread == null, "Already consumed");

        thread = Thread.currentThread(); // volatile field

        MutableInt activeCount = new MutableInt(actors.size());
        boolean[] active = new boolean[actors.size()];
        Arrays.fill(active, true);

        try {
            updateState(State.STARTING);
            LOG.debug("Starting actor");
            startActors(active, activeCount);
            updateState(State.STARTED);
            LOG.debug("Started actor");
            if (activeCount.getValue() == 0) {
                LOG.info("No more actors present in thread, shutting down");
                return;
            }

            InternalEnvelope firstEnv;
            while ((firstEnv = queue.take()) != null) {
                List<InternalEnvelope> envs = new LinkedList<>();
                envs.add(firstEnv);
                queue.drainTo(envs);

                long start = System.currentTimeMillis();
                Instant time = Instant.now();
                for (InternalEnvelope env : envs) {
                    runActor(time, env, active, activeCount);
                    if (activeCount.getValue() == 0) {
                        LOG.info("No more actors present in thread, shutting down");
                        return;
                    }
                }
                long end = System.currentTimeMillis();

                LOG.debug("Processing batch of {} messages took {} ms", queue.size(), end - start);
            }
        } catch (InterruptedException ie) {
            LOG.error("Actor thread interrupted");
            Thread.interrupted(); // clear the interrupted flag because we'll be calling stopActors
        } catch (Exception ex) {
            LOG.error("Actor thread encountered an exception", ex);
        } finally {
            updateState(State.STOPPING);
            LOG.debug("Stopping actor");
            stopActors(active);
            updateState(State.STOPPED);
            LOG.debug("Stopped actor");
        }
    }

    private void updateState(State newState) {
        lock.lock();
        try {
            state.set(newState);

            switch (newState) {
                case STARTING:
                    startingCondition.signalAll();
                    break;
                case STARTED:
                    startedCondition.signalAll();
                    break;
                case STOPPING:
                    stoppingCondition.signalAll();
                    break;
                case STOPPED:
                    stoppedCondition.signalAll();
                    break;
                default:
                    throw new IllegalStateException();
            }
        } finally {
            lock.unlock();
        }
    }

    private void startActors(boolean[] active, MutableInt activeCount) throws InterruptedException {
        int counter = 0;
        for (Actor actor : actors) {
            try {
                actor.onStart(Instant.now());
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                LOG.error("Actor encountered an error on start", e);

                active[counter] = false;
                activeCount.decrement();

                try {
                    actor.onStop(Instant.now());
                } catch (Exception ex) {
                    LOG.error("Actor encountered an error on stop", ex);
                }
            }
            counter++;
        }
    }

    private void stopActors(boolean[] active) {
        int counter = 0;
        for (Actor actor : actors) {
            if (!active[counter]) {
                return;
            }

            try {
                actor.onStop(Instant.now());
            } catch (Exception ex) {
                LOG.error("Actor encountered an error on stop", ex);
            }
        }
    }

    private void runActor(Instant time, InternalEnvelope env, boolean[] active, MutableInt activeCount) throws InterruptedException {
        int actorIndex = env.getActorIndex();

        if (!active[actorIndex]) {
            return;
        }

        Actor actor = actors.get(actorIndex);
        try {
            actor.onStep(time, env.getSource(), env.getMessage());
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Actor encountered an error on run", e);

            active[actorIndex] = false;
            activeCount.decrement();

            try {
                actor.onStop(Instant.now());
            } catch (Exception ex) {
                LOG.error("Actor encountered an error on stop", ex);
            }
        }
    }

    private static final class InternalEnvelope {

        private int actorIndex;
        private Endpoint source;
        private Object message;

        public InternalEnvelope(int actorIndex, Endpoint source, Object message) {
            Validate.isTrue(actorIndex >= 0);
            Validate.notNull(source);
            Validate.notNull(message);

            this.actorIndex = actorIndex;
            this.source = source;
            this.message = message;
        }

        public int getActorIndex() {
            return actorIndex;
        }

        public Endpoint getSource() {
            return source;
        }

        public Object getMessage() {
            return message;
        }

    }

    private static final class InternalEndpoint implements Endpoint {

        private int actorIndex;
        private BlockingQueue<InternalEnvelope> queue;

        public InternalEndpoint(int actorIndex, BlockingQueue<InternalEnvelope> queue) {
            Validate.isTrue(actorIndex >= 0);
            Validate.notNull(queue);

            this.actorIndex = actorIndex;
            this.queue = queue;
        }

        @Override
        public void send(Endpoint source, Object message) {
            InternalEnvelope envelope = new InternalEnvelope(actorIndex, source, message);
            queue.add(envelope);
        }
    }

    private enum State {

        CREATED,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED
    }
}
