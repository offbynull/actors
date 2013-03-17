package com.offbynull.peernetic.eventframework.event;

public interface TrackedIncomingEvent extends IncomingEvent {
    long getTrackedId();
}
