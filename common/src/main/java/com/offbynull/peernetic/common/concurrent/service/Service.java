package com.offbynull.peernetic.common.concurrent.service;


import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycleRunnable;
import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycleAdapter;
import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycle;
import com.offbynull.peernetic.common.concurrent.lifecycle.BlockingLifeCycleListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Service {
    private Thread thread;
    private LifeCycle lifeCycle;
    private BlockingLifeCycleListener listener;

    public Service() {
        this.lifeCycle = new InternalLifeCycle();
        this.thread = new Thread(new LifeCycleRunnable(lifeCycle));
        this.listener = new BlockingLifeCycleListener();
        
        lifeCycle.setListener(listener);
    }

    public Thread getThread() {
        return thread;
    }

    public Future<Void> start() {
        thread.start();
        return listener.awaitStarted();
    }
    
    public Future<Void> stop() {
        boolean stopTriggered = false;
        try {
            stopTriggered = triggerStop();
        } catch (RuntimeException re) { // NOPMD
            // do nothing
        }
        
        if (!stopTriggered) {
            lifeCycle.triggerStop(); // default stop trigger
        }
        
        return listener.awaitStopped();
    }
    
    public void startAndWait() {
        try {
            startAndWaitInterruptibly();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void stopAndWait() {
        try {
            stopAndWaitInterruptibly();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void startAndWaitInterruptibly() throws InterruptedException, ExecutionException {
        start().get();
    }

    public void stopAndWaitInterruptibly() throws InterruptedException, ExecutionException {
        stop().get();
    }

    protected void onStart(Object ... init) throws Exception {
        
    }
    
    protected void onProcess() throws Exception {
        
    }
    
    protected void onStop() throws Exception {
        
    }
    
    protected boolean triggerStop() {
        return false;
    }
    
    private final class InternalLifeCycle extends LifeCycleAdapter {

        @Override
        public void triggerStop() {
            thread.interrupt();
        }

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
