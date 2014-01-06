/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.actor;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * {@link Actor} is an abstract class that should be extended by any class expected to implement the Actor model
 * (http://en.wikipedia.org/wiki/Actor_model).
 * @author Kasra Faghihi
 */
public abstract class Actor {

    private InternalService internalService;
    private boolean daemon;

    private volatile Endpoint endpoint;
    private volatile boolean shutdownRequested;
    private volatile Thread serviceThread;
    
    private Map<Object, Object> startupMap;
    private Lock actorLock; // do not use lock in internalService

    /**
     * Constructs an {@link Actor} object.
     * @param daemon thread daemon
     * @throws IllegalStateException if the internal message pump is {@code null}
     */
    public Actor(boolean daemon) {
        this.daemon = daemon;
        this.actorLock = new ReentrantLock();
        this.startupMap = Collections.synchronizedMap(new HashMap<>());
        this.internalService = new InternalService();
    }
    
    /**
     * Pushes a key-value pair in to the map that gets passed in to {@link #onStart(long, java.util.Map) }.
     * @param key key -- must be immutable
     * @param value value -- must be immutable
     * @throws IllegalStateException if the actor has already been started
     * @throws NullPointerException if {@code key} is {@code null}
     */
    protected final void putInStartupMap(Object key, Object value) {
        Validate.notNull(key);
        
        actorLock.lock();
        try {
            Validate.validState(isNew());
            startupMap.put(key, value);
        } finally {
            actorLock.unlock();
        }
    }
    
    /**
     * Gets the point that can be used to send messages to this actor.
     * @return endpoint for this actor -- {@code null} if not started
     */
    public final Endpoint getEndpoint() {
        return endpoint;
    }
    
    /**
     * Checks to see if this {@link Service} is a new service. That is, checks to see if this service hasn't been started yet.
     * @return {@true} if this service hasn't been started yet, otherwise {@code false}
     */
    public final boolean isNew() {
        return internalService.state() == Service.State.NEW;
    }

    /**
     * Checks to see if this {@link Service} is running. That is, checks to see if this service isn't starting/stopping/stopped.
     * @return {@true} if this service hasn't been started yet, otherwise {@code false}
     */
    public final boolean isRunning() {
        return internalService.state() == Service.State.RUNNING;
    }

    /**
     * Starts this {@link Actor} and waits for it to be ready.
     * @throws IllegalStateException if this method was called previously on this object, or if this actor failed (encountered an exception
     * during execution)
     */
    public final void start() {
        actorLock.lock();
        try {
            internalService.startAsync(); // throws ISE if already started
            internalService.awaitRunning(); // throws ISE if in failed state (exception during startup)
        } finally {
            actorLock.unlock();
        }
    }

    /**
     * Stops this {@link Actor} and waits for it to be finished. If already stopped, this method returns.
     * @throws IllegalStateException if actor is in a failed state (encountered an exception during execution)
     */
    public final void stop() {
        actorLock.lock();
        try {
            internalService.stopAsync(); // throws ISE if already started
            internalService.awaitTerminated(); // throws ISE if in failed state (exception during running or shutdown)
        } finally {
            actorLock.unlock();
        }
    }

    final void readyForTesting() {
        actorLock.lock();
        try {
            Validate.validState(isNew());
            internalService.stopAsync();
        } finally {
            actorLock.unlock();
        }
    }
    
    final void testOnStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        internalService.stopAsync(); // just in case
        onStart(timestamp, pushQueue, initVars);
    }

    final long testOnStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue) throws Exception {
        internalService.stopAsync(); // just in case
        return onStep(timestamp, pullQueue, pushQueue);
    }

    final void testOnStop(long timestamp, PushQueue pushQueue) throws Exception {
        internalService.stopAsync(); // just in case
        onStop(timestamp, pushQueue);
    }
    
    /**
     * Called to initialize this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStop() }.
     * @param timestamp current timestamp
     * @param pushQueue messages to send
     * @param initVars variables to initialize this actor -- values in this map are passed in through
     * {@link #putInStartupMap(java.lang.Object, java.lang.Object) }.
     * @return new queue to use for this actor
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract ActorQueue onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception;

    /**
     * Called when the internal {@link ActorQueueReader} has messages available or the maximum wait duration has elapsed. Called from
     * internally spawned thread (the same thread that called {@link #onStep(long, java.util.Iterator) } and {@link #onStop() }.
     * @param timestamp current timestamp
     * @param pullQueue messages received
     * @param pushQueue messages to send
     * @return maximum amount of time to wait until next invocation of this method, or a negative value to shutdown the service
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue) throws Exception;

    /**
     * Called to shutdown this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStep(long, java.util.Iterator) }.
     * @param timestamp current timestamp
     * @param pushQueue messages to send at the end of each invocation of this method
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract void onStop(long timestamp, PushQueue pushQueue) throws Exception;

    private final class InternalService extends AbstractExecutionThreadService {
        private ActorQueue queue;
        private Endpoint internalEndpoint; // internal copy of parent endpoint, not required to be volatile
        
        @Override
        protected Executor executor() {
            return new Executor() {
              @Override
              public void execute(Runnable command) {
                  Thread thread = Executors.defaultThreadFactory().newThread(command);
                  thread.setName(Actor.this.getClass().getSimpleName());
                  thread.setDaemon(daemon);
                  thread.start();
                  
                  serviceThread = thread;
              }
            };
        }

        @Override
        protected String serviceName() {
            return Actor.this.getClass().getSimpleName();
        }

        @Override
        protected void triggerShutdown() {
            shutdownRequested = true;
            serviceThread.interrupt();
        }

        @Override
        protected void shutDown() throws Exception {
            try {
                PushQueue pushQueue = new PushQueue();
                onStop(System.currentTimeMillis(), pushQueue);
                pushQueue.flush(internalEndpoint);
            } finally {
                queue.close();
            }
        }

        @Override
        protected void startUp() throws Exception {
            Map<Object, Object> internalStartupMap = new HashMap<>(startupMap); // copy incase map is held on to, startUp map being copied
                                                                                // is synchronized -- don't want overhead as this will never
                                                                                // change once it hits starup
            startupMap.clear();

            long startTime = System.currentTimeMillis();
            
            PushQueue pushQueue = new PushQueue();
            queue = onStart(startTime, pushQueue, internalStartupMap);
            internalEndpoint = new LocalEndpoint(queue);
            endpoint = internalEndpoint;
            pushQueue.flush(internalEndpoint);
        }

        @Override
        protected void run() throws Exception {
            ActorQueueReader reader = queue.getReader();
            
            try {
                long nextHitTime = Long.MAX_VALUE;

                while (true) {
                    long pullStartTime = System.currentTimeMillis();
                    long pullWaitDuration = Math.max(nextHitTime - pullStartTime, 0L);
                    Collection<Incoming> messages = reader.pull(pullWaitDuration);

                    PushQueue pushQueue = new PushQueue();
                    PullQueue pullQueue = new PullQueue(messages);
                    long executeStartTime = System.currentTimeMillis();
                    long nextExecuteStartTime = onStep(executeStartTime, pullQueue, pushQueue);
                    pushQueue.flush(internalEndpoint);
                    if (nextExecuteStartTime < 0L) {
                        return;
                    }
                    
                    nextHitTime = nextExecuteStartTime;
                }
            } catch (InterruptedException ie) {
                if (shutdownRequested) {
                    Thread.interrupted(); // clear interrupted status and return from method so shutdown sequence can take place
                } else {
                    throw ie; // shutdown wasn't requested but internal thread was interrupted, push exception up the chain
                }
            }
        }
    }
}
