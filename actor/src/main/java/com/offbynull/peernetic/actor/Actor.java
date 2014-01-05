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
import com.offbynull.peernetic.actor.endpoints.local.LocalEndpoint;
import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

/**
 * {@link Actor} is an abstract class that should be extended by any class expected to implement the Actor model
 * (http://en.wikipedia.org/wiki/Actor_model).
 * @author Kasra Faghihi
 */
public abstract class Actor {

    private InternalService internalService;
    private boolean daemon;

    private volatile ActorQueue queue;
    private volatile boolean shutdownRequested;
    private volatile Thread serviceThread;
    
    private Map<Object, Object> startupMap;
    private Map<Class<? extends Endpoint>, EndpointHandler<?>> endpointHandlerMap;
    private Lock actorLock; // do not use lock in internalService

    /**
     * Constructs an {@link Actor} object.
     * @param daemon thread daemon
     * @throws IllegalStateException if the internal message pump is {@code null}
     */
    public Actor(boolean daemon) {
        Validate.validState(queue != null);
        
        this.daemon = daemon;
        this.actorLock = new ReentrantLock();
        this.startupMap = Collections.synchronizedMap(new HashMap<>());
        this.endpointHandlerMap = Collections.synchronizedMap(new HashMap<Class<? extends Endpoint>, EndpointHandler<?>>());
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
     * Register an endpoint handler with this actor. Must be registered before actor starts.
     * @param <E> endpoint type
     * @param type endpoint type
     * @param handler handler for {@code type}
     * @throws IllegalStateException if the actor has already been started
     * @throws NullPointerException if any argument is {@code null}
     */
    public final <E extends Endpoint> void registerEndpointHandler(Class<E> type, EndpointHandler<E> handler) {
        Validate.notNull(type);
        Validate.notNull(handler);
        
        actorLock.lock();
        try {
            Validate.validState(isNew());
            endpointHandlerMap.put(type, handler);
        } finally {
            actorLock.unlock();
        }
    }
    
    /**
     * Gets the point that can be used to send messages to this actor.
     * @return endpoint for this actor
     * @throws IllegalStateException if actor isn't currently running
     */
    public final Endpoint getEndpoint() {
        actorLock.lock();
        try {
            Validate.validState(internalService.state() == Service.State.RUNNING);
            return new LocalEndpoint(queue);
        } finally {
            actorLock.unlock();
        }
    }
    
    /**
     * Checks to see if this {@link Service} is a new service. That is, checks to see if this service hasn't been started yet.
     * @return {@true} if this service hasn't been started yet, otherwise {@code false}
     */
    public final boolean isNew() {
        return internalService.state() == Service.State.NEW;
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
    
    final void testOnStart(long timestamp, Map<Object, Object> initVars) throws Exception {
        internalService.stopAsync(); // just in case
        onStart(timestamp, initVars);
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
     * Called when the internal {@link ActorQueueReader} has messages available or the maximum wait duration has elapsed. Called from
     * internally spawned thread (the same thread that called {@link #onStep(long, java.util.Iterator) } and {@link #onStop() }.
     * @param timestamp current timestamp
     * @param pullQueue messages received
     * @param pushQueue messages to sen
     * @return maximum amount of time to wait until next invocation of this method, or a negative value to shutdown the service
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue) throws Exception;
    
    /**
     * Called to initialize this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStop() }.
     * @param timestamp current timestamp
     * @param initVars variables to initialize this actor -- values in this map are passed in through
     * {@link #putInStartupMap(java.lang.Object, java.lang.Object) }.
     * @return new queue to use for this actor
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract ActorQueue onStart(long timestamp, Map<Object, Object> initVars) throws Exception;

    /**
     * Called to shutdown this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStep(long, java.util.Iterator) }.
     * @param timestamp current timestamp
     * @param pushQueue messages to send at the end of each invocation of this method
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract void onStop(long timestamp, PushQueue pushQueue) throws Exception;

    private final class InternalService extends AbstractExecutionThreadService {
        private IdCounter requestIdCounter = new IdCounter();
        private TimeoutManager<Object> requestIdTracker = new TimeoutManager<>(); // tracks ids and times them out
        private Map<Class<? extends Endpoint>, EndpointHandler<?>> internalEndpointHandlerMap;
        
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
            queue.close();
            shutdownRequested = true;
            serviceThread.interrupt();
        }

        @Override
        protected void shutDown() throws Exception {
            MultiMap<Endpoint, Outgoing> outgoingMap = new MultiValueMap<>();
            
            PushQueue pushQueue = new PushQueue(requestIdCounter, requestIdTracker, outgoingMap);
            onStop(System.currentTimeMillis(), pushQueue);

            sendOutgoing(outgoingMap);
        }

        @Override
        protected void startUp() throws Exception {
            Map<Object, Object> internalStartupMap = new HashMap<>(startupMap); // copy incase map is held on to, startUp map being copied
                                                                                // is synchronized -- don't want overhead as this will never
                                                                                // change once it hits starup
            internalEndpointHandlerMap = new HashMap<>(endpointHandlerMap); // copy so we are using our own version, endpointHandlerMap
                                                                            // being copied is synchronized -- don't want overhead as this
                                                                            // will never change once it hits starup
            startupMap.clear();
            endpointHandlerMap.clear();
            queue = onStart(System.currentTimeMillis(), internalStartupMap);
        }

        @Override
        protected void run() throws Exception {
            ActorQueueReader reader = queue.getReader();
            
            try {
                long waitUntil = Long.MAX_VALUE;

                while (true) {
                    long processTimeoutsTime = System.currentTimeMillis();
                    long nextResponseTimeoutTime = requestIdTracker.process(processTimeoutsTime).getNextTimeoutTimestamp();
                    waitUntil = Math.min(waitUntil, nextResponseTimeoutTime);
                    
                    Collection<Incoming> messages = reader.pull(waitUntil);

                    MultiMap<Endpoint, Outgoing> outgoingMap = new MultiValueMap<>();
                    PushQueue pushQueue = new PushQueue(requestIdCounter, requestIdTracker, outgoingMap);
                    PullQueue pullQueue = new PullQueue(requestIdTracker, messages);

                    long startStepTime = System.currentTimeMillis();
                    long nextStepTime = onStep(startStepTime, pullQueue, pushQueue);
                    
                    sendOutgoing(outgoingMap);
                    
                    if (nextStepTime < 0L) {
                        return;
                    }

                    long stopStepTime = System.currentTimeMillis();
                    waitUntil = Math.max(stopStepTime - nextStepTime, 0L);
                }
            } catch (InterruptedException ie) {
                if (shutdownRequested) {
                    Thread.interrupted(); // clear interrupted status and return from method so shutdown sequence can take place
                } else {
                    throw ie; // shutdown wasn't requested but internal thread was interrupted, push exception up the chain
                }
            }
        }
        
        private void sendOutgoing(MultiMap<Endpoint, Outgoing> outgoingMap) {
            for (Map.Entry<Endpoint, Object> entry : outgoingMap.entrySet()) {
                Endpoint dstEndpoint = entry.getKey();
                Collection<Outgoing> outgoing = (Collection<Outgoing>) entry.getValue();

                EndpointHandler handler = internalEndpointHandlerMap.get(dstEndpoint.getClass());
                if (handler != null) {
                    handler.push(getEndpoint(), dstEndpoint, outgoing);
                }
            }
        }
    }
}
