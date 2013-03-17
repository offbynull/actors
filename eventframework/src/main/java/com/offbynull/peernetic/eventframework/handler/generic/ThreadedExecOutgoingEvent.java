package com.offbynull.peernetic.eventframework.handler.generic;

import com.offbynull.peernetic.eventframework.event.DefaultTrackedOutgoingEvent;
import java.util.concurrent.Callable;

public final class ThreadedExecOutgoingEvent extends DefaultTrackedOutgoingEvent {
    private Callable<?> callable;

    public ThreadedExecOutgoingEvent(Callable<?> callable, long trackedId) {
        super(trackedId);
        
        if (callable == null) {
            throw new NullPointerException();
        }
        
        this.callable = callable;
    }
    
    public ThreadedExecOutgoingEvent(final Runnable runnable, long trackedId) {
        super(trackedId);
        
        if (runnable == null) {
            throw new NullPointerException();
        }
        
        this.callable = new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        };
    }

    public Callable<?> getCallable() {
        return callable;
    }
}
