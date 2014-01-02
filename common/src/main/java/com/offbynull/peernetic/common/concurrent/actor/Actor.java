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
package com.offbynull.peernetic.common.concurrent.actor;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import java.util.Iterator;
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

    private ActorQueue queue;
    private InternalService internalService;
    private boolean daemon;
    
    private volatile boolean shutdownRequested;
    private volatile Thread serviceThread;
    
    private Lock lock;

    /**
     * Constructs an {@link Actor} object.
     * @param daemon thread daemon
     * @throws IllegalStateException if the internal message pump is {@code null}
     */
    public Actor(boolean daemon) {
        this.queue = createQueue();
        Validate.validState(queue != null);
        
        this.daemon = daemon;
        this.lock = new ReentrantLock();
        this.internalService = new InternalService();
    }

    /**
     * Create the internal message queue. Called from constructor.
     * @return a new message queue to be used by this actor, cannot be {@code null}
     */
    protected abstract ActorQueue createQueue();

    /**
     * Checks to see if this {@link Service} is a new service. That is, checks to see if this service hasn't been started yet.
     * @return 
     */
    public final boolean isNew() {
        return internalService.state() != Service.State.NEW;
    }
    
    /**
     * Starts this {@link Actor} and waits for it to be ready.
     * @throws IllegalStateException if this method was called previously on this object, or if this actor failed (encountered an exception
     * during execution)
     */
    public final void start() {
        lock.lock();
        try {
            internalService.startAsync(); // throws ISE if already started
            internalService.awaitRunning(); // throws ISE if in failed state (exception during startup)
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops this {@link Actor} and waits for it to be finished. If already stopped, this method returns.
     * @throws IllegalStateException if actor is in a failed state (encountered an exception during execution)
     */
    public final void stop() {
        lock.lock();
        try {
            internalService.stopAsync(); // throws ISE if already started
            internalService.awaitTerminated(); // throws ISE if in failed state (exception during running or shutdown)
        } finally {
            lock.unlock();
        }
    }

    /**
     * Called when the internal {@link ActorQueueReader} has messages available or the maximum wait duration has elapsed. Called from
     * internally spawned thread (the same thread that called {@link #onStep(long, java.util.Iterator) } and {@link #onStop() }.
     * @param timestamp current timestamp
     * @param iterator messages from the internal {@link ActorQueueReader}
     * @param pushQueue messages to send at the end of each invocation of this method
     * @return maximum amount of time to wait until next invokation of this method, or a negative value to shutdown the service
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract long onStep(long timestamp, Iterator<Message> iterator, PushQueue pushQueue) throws Exception;
    
    /**
     * Called to initialize this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStop() }.
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract void onStart() throws Exception;

    /**
     * Called to shutdown this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStep(long, java.util.Iterator) }.
     * @param pushQueue messages to send at the end of each invocation of this method
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract void onStop(PushQueue pushQueue) throws Exception;

    private final class InternalService extends AbstractExecutionThreadService {
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
            PushQueue responseQueue = new PushQueue(queue.getWriter());
            onStop(responseQueue);
            responseQueue.flush();
        }

        @Override
        protected void startUp() throws Exception {
            onStart();
        }

        @Override
        protected void run() throws Exception {
            ActorQueueReader reader = queue.getReader();
            
            try {
                long waitUntil = Long.MAX_VALUE;

                PushQueue responseQueue = new PushQueue(queue.getWriter());
                while (true) {
                    Iterator<Message> messages = reader.pull(waitUntil);

                    long preStepTime = System.currentTimeMillis();
                    long nextStepTime = onStep(preStepTime, messages, responseQueue);

                    responseQueue.flush();
                    
                    if (nextStepTime < 0L) {
                        return;
                    }

                    long postStepTime = System.currentTimeMillis();

                    waitUntil = Math.max(nextStepTime - postStepTime, 0L);
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
