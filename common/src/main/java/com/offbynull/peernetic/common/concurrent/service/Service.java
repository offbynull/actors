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
package com.offbynull.peernetic.common.concurrent.service;


import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycleRunnable;
import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycle;
import com.offbynull.peernetic.common.concurrent.lifecycle.BlockingLifeCycleListener;
import com.offbynull.peernetic.common.concurrent.lifecycle.CompositeLifeCycleListener;
import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycleState;
import com.offbynull.peernetic.common.concurrent.lifecycle.RetainStateLifeCycleListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * {@link Service} is an abstract class that should be extended by any class whose instances are to be executed in their own thread. Like
 * {@code LifeCycle}, this class provides methods for start-up and shutdown phases.
 * <p/>
 * Classes that extend this abstract should expect {@link #onStart(java.lang.Object...) } to get called first,
 * then {@link #onProcess() }, then finally {@link #onStop() }. In the event of exceptions thrown by these methods...
 * <ul>
 * <li>If {@link #onStart(java.lang.Object...) } throws an exception, expect {@link #onProcess() } to be skipped.</li>
 * <li>If {@link #onProcess() } throws an exception, expect to move on to {@link #onStop() }.</li>
 * <li>The executing thread should complete regardless of if {@link #onStop() } throws an exception.</li>
 * </ul>
 * <p/>
 * Implementations should be designed to run once. That is, subsequent runs of the same instance will encounter an exception.
 * <p/>
 * Expect the thread internal to this instance to run {@link #onStart(java.lang.Object...) }, {@link #onProcess() }, and {@link #onStop() }.
 * @author Kasra Faghihi
 */
public abstract class Service {
    private Thread thread;
    private LifeCycle lifeCycle;
    private BlockingLifeCycleListener blockingListener;
    private RetainStateLifeCycleListener retainStateListener;
    private ServiceStopTrigger stopTrigger;
    private Lock lock;
    private boolean stopped;

    /**
     * Constructs a {@link Service} object. Default stop trigger used service thread.
     * @param name thread name
     * @param daemon thread daemon
     * @throws NullPointerException if any argument is {@code null}
     */
    public Service(String name, boolean daemon) {
        initialize(name, daemon, new CustomServiceStopTrigger());
    }

    /**
     * Constructs a {@link Service} object with a custom stop trigger.
     * @param name thread name
     * @param daemon thread daemon
     * @param stopTrigger stop trigger
     * @throws NullPointerException if any argument is {@code null}
     */
    public Service(String name, boolean daemon, ServiceStopTrigger stopTrigger) {
        initialize(name, daemon, stopTrigger);
    }

    private void initialize(String name, boolean daemon, ServiceStopTrigger stopTrigger) {
        Validate.notNull(stopTrigger);
        Validate.notNull(name);
        
        this.lifeCycle = new InternalLifeCycle();
        this.blockingListener = new BlockingLifeCycleListener();
        this.retainStateListener = new RetainStateLifeCycleListener();
        this.thread = new Thread(new LifeCycleRunnable(lifeCycle, new CompositeLifeCycleListener(blockingListener, retainStateListener)));
        this.stopTrigger = stopTrigger;
        this.lock = new ReentrantLock();
        
        thread.setName(name);
        thread.setDaemon(daemon);
    }
    
    /**
     * Gets the thread used by this service.
     * @return thread used by this service
     */
    public final Thread getThread() {
        return thread;
    }

    /**
     * Get the state of this service.
     * @return state of this service
     */
    public final LifeCycleState getState() {
        return retainStateListener.getState();
    }

    /**
     * Start this service and wait until {@link #onStart(java.lang.Object...) } has completed or failed.
     * @throws InterruptedException if this thread was interrupted while waiting
     * @throws IllegalStateException if this service has already been run once, or if the service failed during startup
     */
    public final void start() throws InterruptedException {
        Validate.validState(retainStateListener.getState() == null);
        
        lock.lock();
        try {
            thread.start();
            try {
                blockingListener.awaitStarted().get();
            } catch (ExecutionException ee) { // NOPMD
                // never happens
            }
            
            Validate.validState(!retainStateListener.isFailed());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Triggers this service to stop and waits until {@link #onStop() } has completed or failed.
     * @throws InterruptedException if this thread was interrupted while waiting
     * @throws IllegalStateException if this service has already been run once, if it has already been stopped, or if it hasn't been run yet
     */
    public final void stop() throws InterruptedException {
        Validate.validState(retainStateListener.getState() != null);
        
        lock.lock();
        try {
            Validate.validState(!stopped);
            stopped = true;

            stopTrigger.triggerStop();
            
            try {
                blockingListener.awaitStopped().get();
            } catch (ExecutionException ee) { // NOPMD
                // never happens
            }
            
            Validate.validState(!retainStateListener.isFailed());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Start-up / initialization. Called by internal thread.
     * @param init start-up variables
     * @throws Exception on error -- if encountered the next method called will be {@link #onStop() }
     */
    protected void onStart(Object ... init) throws Exception {
        
    }
    
    /**
     * Process. Called by internal thread.
     * @throws Exception on error
     */
    protected abstract void onProcess() throws Exception;
    
    /**
     * Shutdown. Called by internal thread.
     * @throws Exception on error
     */
    protected void onStop() throws Exception {
        
    }
    
    private final class CustomServiceStopTrigger implements ServiceStopTrigger {
        
        @Override
        public void triggerStop() {
            thread.interrupt();
        }
    }
    
    private final class InternalLifeCycle implements LifeCycle {

        @Override
        public void onStop() throws Exception {
            Service.this.onStop();
        }

        @Override
        public void onProcess() throws Exception {
            Service.this.onProcess();
        }

        @Override
        public void onStart(Object... init) throws Exception {
            Service.this.onStart(init);
        }
        
    }
}
