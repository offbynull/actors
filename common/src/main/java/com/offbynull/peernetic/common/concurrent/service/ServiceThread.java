package com.offbynull.peernetic.common.concurrent.service;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class ServiceThread {
    private Thread thread;
    private Service service;
    private BlockingServiceListener listener;

    public ServiceThread(Service service) {
        this.service = service;
        this.thread = new Thread(new ServiceRunnable(service));
        this.listener = new BlockingServiceListener();
        
        service.setListener(listener);
    }

    public Thread getThread() {
        return thread;
    }

    public Future<Void> start() {
        thread.start();
        return listener.awaitStarted();
    }
    
    public Future<Void> stop() {
        service.triggerStop();
        return listener.awaitStopped();
    }
    
    public void startAndWait() {
        try {
            start().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void stopAndWait() {
        try {
            service.triggerStop();
            stop().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
