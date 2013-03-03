package com.offbynull.peernetic.eventframework.handler;

public interface TrackedOutgoingEvent extends OutgoingEvent {
    long getTrackedId();
}
