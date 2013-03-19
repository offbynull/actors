package com.offbynull.peernetic.eventframework.event;

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
