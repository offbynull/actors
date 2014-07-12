package com.offbynull.peernetic.actor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

public final class ActorRunnable implements Runnable {

    private final UnmodifiableList<Actor> actors;
    private final UnmodifiableMap<Actor, Endpoint> endpoints;
    private final LinkedBlockingQueue<InternalEnvelope> queue;
    private final Lock lock;
    private final Condition startCondition;
    private volatile Thread thread;

    public static ActorRunnable createAndStart(Actor... actors) {
        ActorRunnable actorRunnable = new ActorRunnable(actors);

        actorRunnable.lock.lock();
        try {
            Thread actorThread = new Thread(actorRunnable);
            actorThread.start();

            actorRunnable.startCondition.awaitUninterruptibly();
        } finally {
            actorRunnable.lock.unlock();
        }

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
        this.lock = new ReentrantLock();
        this.startCondition = lock.newCondition();
    }

    public void awaitRunning() {
        if (thread != null) {
            return;
        }

        lock.lock();
        try {
            if (thread != null) {
                return;
            }
            startCondition.awaitUninterruptibly();
        } finally {
            lock.unlock();
        }
    }

    public Endpoint getEndpoint(Actor actor) {
        Validate.validState(thread != null);
        Validate.notNull(actor);

        Endpoint endpoint = endpoints.get(actor);
        Validate.isTrue(endpoint != null, "Actor not found");
        
        return endpoint;
    }

    public Thread getThread() {
        Validate.validState(thread != null);

        return thread;
    }

    public void shutdown() throws InterruptedException {
        Validate.validState(thread != null);

        thread.interrupt();
        thread.join();
    }

    @Override
    public void run() {
        Validate.validState(thread == null, "Already consumed");

        lock.lock();
        try {
            thread = Thread.currentThread();
            startCondition.signal();
        } finally {
            lock.unlock();
        }

        MutableInt activeCount = new MutableInt(actors.size());
        boolean[] active = new boolean[actors.size()];
        Arrays.fill(active, true);

        try {
            startActors(active, activeCount);
            if (activeCount.getValue() == 0) {
                return;
            }

            InternalEnvelope env;
            while ((env = queue.take()) != null) {
                runActor(env, active, activeCount);
                if (activeCount.getValue() == 0) {
                    return;
                }
            }
        } catch (InterruptedException ie) {
            // TODO: Log here
            Thread.interrupted(); // clear the interrupted flag because we'll be calling stopActors
        } catch (Exception ex) {
            System.err.println(ex);
            // TODO: Log here
        } finally {
            stopActors(active);
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
                // TODO: Log here
                
                active[counter] = false;
                activeCount.decrement();

                try {
                    actor.onStop(Instant.now());
                } catch (Exception ex) {
                    System.err.println(ex);
                    // TODO: Log here
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
                System.err.println(ex);
                // TODO: Log here
            }
        }
    }

    private void runActor(InternalEnvelope env, boolean[] active, MutableInt activeCount) throws InterruptedException {
        int actorIndex = env.getActorIndex();

        if (!active[actorIndex]) {
            return;
        }

        Actor actor = actors.get(actorIndex);
        try {
            actor.onStep(Instant.now(), env.getSource(), env.getMessage());
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            // TODO: Log here
            
            active[actorIndex] = false;
            activeCount.decrement();

            try {
                actor.onStop(Instant.now());
            } catch (Exception ex) {
                System.err.println(ex);
                // TODO: Log here
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
}
