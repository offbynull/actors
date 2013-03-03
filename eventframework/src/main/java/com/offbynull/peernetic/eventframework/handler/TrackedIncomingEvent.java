package com.offbynull.peernetic.eventframework.handler;

public interface TrackedIncomingEvent extends IncomingEvent {
    long getTrackedId();
}
