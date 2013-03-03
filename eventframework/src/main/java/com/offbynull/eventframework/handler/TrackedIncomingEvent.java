package com.offbynull.eventframework.handler;

public interface TrackedIncomingEvent extends IncomingEvent {
    long getTrackedId();
}
