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
    private volatile Thread thread;

    public static ActorRunnable createAndStart(Actor... actors) {
        ActorRunnable actorRunnable = new ActorRunnable(actors);

        Thread actorThread = new Thread(actorRunnable);
        actorThread.setDaemon(true);
        actorThread.setName(ActorRunnable.class.getSimpleName() + "-Thread");
        actorThread.start();

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
    }

    public Endpoint getEndpoint(Actor actor) {
        Validate.notNull(actor);

        Endpoint endpoint = endpoints.get(actor);
        Validate.isTrue(endpoint != null, "Actor not found");

        return endpoint;
    }

    public void shutdown() throws InterruptedException {
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
            InternalEnvelope firstEnv;
            while ((firstEnv = queue.take()) != null) {
                List<InternalEnvelope> envs = new LinkedList<>();
                envs.add(firstEnv);
                queue.drainTo(envs);

                long start = System.currentTimeMillis();
                Instant time = Instant.now();
                for (InternalEnvelope env : envs) {
                    if (LOG.isTraceEnabled()) {
                        Actor actor = actors.get(env.getActorIndex());
                        Endpoint selfEndpoint = endpoints.get(actor);
                        LOG.trace("Processing message from {} to {}: {}", env.getSource(), selfEndpoint, env.getMessage());
                    }

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
