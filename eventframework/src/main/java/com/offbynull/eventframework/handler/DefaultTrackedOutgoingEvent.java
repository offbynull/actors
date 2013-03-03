package com.offbynull.eventframework.handler;

public abstract class DefaultTrackedOutgoingEvent implements TrackedOutgoingEvent {
    private long trackedId;

    public DefaultTrackedOutgoingEvent(long trackedId) {
        this.trackedId = trackedId;
    }

    @Override
    public long getTrackedId() {
        return trackedId;
    }
}
