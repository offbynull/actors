package com.offbynull.peernetic.eventframework.basic.timer;

import com.offbynull.peernetic.eventframework.event.DefaultTrackedIncomingEvent;

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
