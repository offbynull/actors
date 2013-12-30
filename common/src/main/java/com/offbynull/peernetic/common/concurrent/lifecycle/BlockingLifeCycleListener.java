package com.offbynull.peernetic.common.concurrent.lifecycle;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockingLifeCycleListener implements LifeCycleListener {

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
                stoppingCl.countDown();
                finishedCl.countDown();
                break;
        }
    }

    public Future<Void> awaitStarted() {
        return new CountDownLatchFuture(processingCl);
    }
    
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
