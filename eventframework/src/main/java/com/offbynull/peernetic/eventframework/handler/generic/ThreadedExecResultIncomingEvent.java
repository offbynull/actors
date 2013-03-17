package com.offbynull.peernetic.eventframework.handler.generic;

import com.offbynull.peernetic.eventframework.event.DefaultSuccessIncomingEvent;

public final class ThreadedExecResultIncomingEvent extends DefaultSuccessIncomingEvent {
    
    private Object result;
    
    public ThreadedExecResultIncomingEvent(long trackedId, Object result) {
        super(trackedId);
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
    
}
