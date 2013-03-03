package com.offbynull.eventframework.handler;

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
