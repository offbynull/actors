package com.offbynull.peernetic.eventframework.impl.timer;

import com.offbynull.peernetic.eventframework.event.DefaultTrackedOutgoingEvent;

public final class NewTimerOutgoingEvent extends DefaultTrackedOutgoingEvent {
    private long duration;
    private Object data;

    public NewTimerOutgoingEvent(long trackedId, long duration) {
        this(trackedId, duration, null);
    }

    public NewTimerOutgoingEvent(long trackedId, long duration,
            Object data) {
        super(trackedId);
        
        if (duration < 0L) {
            throw new IllegalArgumentException();
        }
        this.duration = duration;
        this.data = data; // data can be null
    }

    public long getDuration() {
        return duration;
    }

    public Object getData() {
        return data;
    }
}
