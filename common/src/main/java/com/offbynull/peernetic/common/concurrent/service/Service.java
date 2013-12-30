package com.offbynull.peernetic.common.concurrent.service;


import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycleRunnable;
import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycleAdapter;
import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycle;
import com.offbynull.peernetic.common.concurrent.lifecycle.BlockingLifeCycleListener;
import com.offbynull.peernetic.common.concurrent.lifecycle.CompositeLifeCycleListener;
import com.offbynull.peernetic.common.concurrent.lifecycle.LifeCycleState;
import com.offbynull.peernetic.common.concurrent.lifecycle.RetainStateLifeCycleListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public abstract class Service {
    private Thread thread;
    private LifeCycle lifeCycle;
    private BlockingLifeCycleListener blockingListener;
    private RetainStateLifeCycleListener retainStateListener;
    private ServiceStopTrigger stopTrigger;
    private Lock lock;

    public Service(String name, boolean daemon) {
        initialize(name, daemon, new CustomServiceStopTrigger());
    }
    
    public Service(String name, boolean daemon, ServiceStopTrigger stopTrigger) {
        initialize(name, daemon, stopTrigger);
    }

    private void initialize(String name, boolean daemon, ServiceStopTrigger stopTrigger) {
        Validate.notNull(stopTrigger);
        Validate.notNull(name);
        
        this.lifeCycle = new InternalLifeCycle();
        this.thread = new Thread(new LifeCycleRunnable(lifeCycle));
        this.blockingListener = new BlockingLifeCycleListener();
        this.retainStateListener = new RetainStateLifeCycleListener();
        this.stopTrigger = stopTrigger;
        this.lock = new ReentrantLock();
        
        thread.setName(name);
        thread.setDaemon(daemon);
        
        lifeCycle.setListener(new CompositeLifeCycleListener(blockingListener, retainStateListener));
    }
    
    public Thread getThread() {
        return thread;
    }

    public LifeCycleState getState() {
        return retainStateListener.getState();
    }

    public Future<Void> start() {
        lock.lock();
        
        Validate.validState(retainStateListener.getState() == LifeCycleState.CREATED);
        
        try {
            thread.start();
            return blockingListener.awaitStarted();
        } finally {
            lock.unlock();
        }
    }
    
    public Future<Void> stop() {
        lock.lock();
        
        Validate.validState(retainStateListener.getState() != LifeCycleState.FINISHED);
        
        try {
            triggerStop();

            return blockingListener.awaitStopped();
        } finally {
            lock.unlock();
        }
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
    
    protected abstract void onProcess() throws Exception;
    
    protected void onStop() throws Exception {
        
    }
    
    public final void triggerStop() {
        stopTrigger.triggerStop();
    }
    
    private final class CustomServiceStopTrigger implements ServiceStopTrigger {
        
        @Override
        public void triggerStop() {
            lifeCycle.triggerStop();
        }
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
