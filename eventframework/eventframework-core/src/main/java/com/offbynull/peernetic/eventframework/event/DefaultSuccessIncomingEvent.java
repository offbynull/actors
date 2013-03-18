package com.offbynull.peernetic.eventframework.event;

public class DefaultSuccessIncomingEvent implements SuccessIncomingEvent {
    private long trackedId;

    public DefaultSuccessIncomingEvent(long trackedId) {
        this.trackedId = trackedId;
    }

    @Override
    public long getTrackedId() {
        return trackedId;
    }
}
