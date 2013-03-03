package com.offbynull.eventframework.handler;

public abstract class DefaultTrackedIncomingEvent implements TrackedIncomingEvent {
    private long trackedId;

    public DefaultTrackedIncomingEvent(long trackedId) {
        this.trackedId = trackedId;
    }

    @Override
    public long getTrackedId() {
        return trackedId;
    }
}
