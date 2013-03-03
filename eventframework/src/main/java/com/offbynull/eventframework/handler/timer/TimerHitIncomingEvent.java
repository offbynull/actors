package com.offbynull.eventframework.handler.timer;

import com.offbynull.eventframework.handler.DefaultTrackedIncomingEvent;

public final class TimerHitIncomingEvent extends DefaultTrackedIncomingEvent {
    
    private Object data;
    
    public TimerHitIncomingEvent(long trackedId, Object data) {
        super(trackedId);
        this.data = data;
    }

    public Object getData() {
        return data;
    }
    
}
