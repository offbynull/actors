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
package com.offbynull.peernetic.common.concurrent.lifecycle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link LifeCycleListener} that provides a mechanism to block until execution of a {@link LifeCycle} has started or stopped.
 * @author Kasra Faghihi
 */
public final class BlockingLifeCycleListener implements LifeCycleListener {

    private CountDownLatch startingCl = new CountDownLatch(1);
    private CountDownLatch processingCl = new CountDownLatch(1);
    private CountDownLatch stoppingCl = new CountDownLatch(1);
    private CountDownLatch finishedCl = new CountDownLatch(1);
    private volatile boolean failed;

    @Override
    public void stateChanged(LifeCycle service, LifeCycleState state) {
        switch (state) {
            case STARTING:
                startingCl.countDown();
                break;
            case PROCESSING:
                processingCl.countDown();
                break;
            case STOPPING:
                stoppingCl.countDown();
                break;
            case FINISHED:
                finishedCl.countDown();
                break;
            case FAILED:
                failed = true;
                startingCl.countDown();
                processingCl.countDown();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Gets a {@link Future} that will block until {@link LifeCycleState#PROCESSING} is reached. Cannot be canceled. If
     * {@link LifeCycleState#PROCESSING} has already been reached, the {@link Future} returns immediately.
     * @return a {@link Future} that waits until {@link LifeCycleState#PROCESSING} is reached
     */
    public Future<Void> awaitStarted() {
        return new CountDownLatchFuture(processingCl);
    }

    /**
     * Gets a {@link Future} that will block until {@link LifeCycleState#FINISHED} is reached. Cannot be canceled. If
     * {@link LifeCycleState#FINISHED} has already been reached, the {@link Future} returns immediately.
     * @return a {@link Future} that waits until {@link LifeCycleState#FINISHED} is reached
     */
    public Future<Void> awaitStopped() {
        return new CountDownLatchFuture(finishedCl);
    }

    private class CountDownLatchFuture implements Future<Void> {

        private CountDownLatch latch;
        
        public CountDownLatchFuture(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return failed;
        }

        @Override
        public boolean isDone() {
            return latch.getCount() == 0;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            latch.await();
            if (failed) {
                throw new ExecutionException(null);
            }
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            latch.await(timeout, unit);
            if (failed) {
                throw new ExecutionException(null);
            }
            return null;
        }
    }
}
