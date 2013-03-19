package com.offbynull.peernetic.eventframework.event;

public interface TrackedOutgoingEvent extends OutgoingEvent {
    long getTrackedId();
}
