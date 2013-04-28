package com.offbynull.peernetic.eventframework.event;

public class DefaultSuccessIncomingEvent implements SuccessIncomingEvent {
    private long trackedId;
    private Object data;

    public DefaultSuccessIncomingEvent(long trackedId) {
        this.trackedId = trackedId;
        this.data = null;
    }
    
    public <T> DefaultSuccessIncomingEvent(long trackedId, T data) {
        this.trackedId = trackedId;
        this.data = data;
    }

    @Override
    public long getTrackedId() {
        return trackedId;
    }
    
    public Object getData() {
        return data;
    }
}
