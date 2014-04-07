/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * {@link ActorRunner} is used to start and run {@link Actor}s. See (http://en.wikipedia.org/wiki/Actor_model) for more information on
 * actors.
 * @author Kasra Faghihi
 */
public final class ActorRunner {

    private InternalService internalService;
    private Actor internalActor;
    private boolean daemon;

    private volatile Endpoint endpoint; // endpoint implementation is thread safe
    private volatile boolean shutdownRequested;
    private volatile Thread serviceThread;
    
    private Lock actorLock; // do not use lock in internalService

    /**
     * Wraps an {@link Actor} in an {@link ActorRunner} and starts it.
     * @param actor actor to wrap
     * @return {@link ActorRunner} that wraps {@code} actor and has has {@link #start() } invoked
     */
    public static ActorRunner createAndStart(Actor actor) {
        Validate.notNull(actor);
        
        ActorRunner runner = new ActorRunner(true, actor);
        runner.start();
        return runner;
    }
    /**
     * Constructs an {@link Actor} object.
     * @param daemon thread daemon
     * @param actor actor to run
     * @throws NullPointerException if any argument is {@code null}
     */
    public ActorRunner(boolean daemon, Actor actor) {
        Validate.notNull(actor);
        
        this.daemon = daemon;
        this.actorLock = new ReentrantLock();
        this.internalService = new InternalService();
        this.internalActor = actor;
    }
    
    /**
     * Gets the point that can be used to send messages to this actor.
     * @return endpoint for this actor -- {@code null} if not started
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }
    
    /**
     * Checks to see if this {@link Service} is a new service. That is, checks to see if this service hasn't been started yet.
     * @return {@true} if this service hasn't been started yet, otherwise {@code false}
     */
    public boolean isNew() {
        return internalService.state() == Service.State.NEW;
    }

    /**
     * Checks to see if this {@link Service} is running. That is, checks to see if this service isn't starting/stopping/stopped.
     * @return {@true} if this service hasn't been started yet, otherwise {@code false}
     */
    public boolean isRunning() {
        return internalService.state() == Service.State.RUNNING;
    }

    /**
     * Starts this {@link ActorRunner} and waits for it to be ready.
     * @throws IllegalStateException if this method was called previously on this object, or if this actor failed (encountered an exception
     * during execution)
     */
    public void start() {
        actorLock.lock();
        try {
            internalService.startAsync(); // throws ISE if already started
            internalService.awaitRunning(); // throws ISE if in failed state (exception during startup)
        } finally {
            actorLock.unlock();
        }
    }

    /**
     * Stops this {@link ActorRunner} and waits for it to be finished. If already stopped, this method returns.
     * @throws IllegalStateException if actor is in a failed state (encountered an exception during execution)
     */
    public void stop() {
        actorLock.lock();
        try {
            internalService.stopAsync(); // throws ISE if already started
            internalService.awaitTerminated(); // throws ISE if in failed state (exception during running or shutdown)
        } finally {
            actorLock.unlock();
        }
    }

    private final class InternalService extends AbstractExecutionThreadService {
        private ActorQueue queue;
        private LocalEndpoint internalEndpoint; // internal copy of parent endpoint, not required to be volatile
        private long nextHitTime;
        
        @Override
        protected Executor executor() {
            return new Executor() {
              @Override
              public void execute(Runnable command) {
                  Thread thread = new Thread(command);
                  thread.setDaemon(daemon);
                  thread.start();
                  
                  serviceThread = thread;
              }
            };
        }

        @Override
        protected String serviceName() {
            return internalActor.getClass().getSimpleName();
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
                internalActor.onStop(System.currentTimeMillis(), pushQueue);
                pushQueue.flush(internalEndpoint);
            } finally {
                queue.close();
            }
        }

        @Override
        protected void startUp() throws Exception {
            Map<Object, Object> internalStartupMap = internalActor.consume();
            long startTime = System.currentTimeMillis();
            
            PushQueue pushQueue = new PushQueue();
            ActorStartSettings settings = internalActor.onStart(startTime, pushQueue, internalStartupMap);
            
            queue = new ActorQueue();
            internalEndpoint = new LocalEndpoint(queue);
            endpoint = internalEndpoint;
            
            for (Object selfOutgoing : settings.getMessagesToSelf()) {
                pushQueue.push(internalEndpoint, selfOutgoing);
            }
            
            pushQueue.flush(internalEndpoint);
            
            nextHitTime = settings.getHitTime();
        }

        @Override
        protected void run() throws Exception {
            try {
                while (true) {
                    long pullStartTime = System.currentTimeMillis();
                    long pullWaitDuration = Math.max(nextHitTime - pullStartTime, 0L);
                    Collection<Incoming> messages = queue.pull(pullWaitDuration);

                    PushQueue pushQueue = new PushQueue();
                    PullQueue pullQueue = new PullQueue(messages);
                    long executeStartTime = System.currentTimeMillis();
                    long nextExecuteStartTime = internalActor.onStep(executeStartTime, pullQueue, pushQueue, internalEndpoint);
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
