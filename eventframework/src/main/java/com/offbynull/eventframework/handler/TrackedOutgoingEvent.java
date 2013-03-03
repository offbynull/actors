package com.offbynull.eventframework.handler;

public interface TrackedOutgoingEvent extends OutgoingEvent {
    long getTrackedId();
}
