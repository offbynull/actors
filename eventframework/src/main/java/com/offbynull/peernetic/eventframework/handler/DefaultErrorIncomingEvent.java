package com.offbynull.peernetic.eventframework.handler;

public class DefaultErrorIncomingEvent implements ErrorIncomingEvent {
    private long trackedId;
    private String description;
    private Throwable throwable;

    public DefaultErrorIncomingEvent(long trackedId) {
        this(trackedId, null);
    }

    public DefaultErrorIncomingEvent(long trackedId, Throwable throwable) {
        this(trackedId, throwable != null ? throwable.getMessage() : null,
                throwable);
    }

    public DefaultErrorIncomingEvent(long trackedId, String description,
            Throwable throwable) {
        this.description = description;
        this.trackedId = trackedId;
        this.throwable = throwable;
    }

    @Override
    public long getTrackedId() {
        return trackedId;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
