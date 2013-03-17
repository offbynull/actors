package com.offbynull.peernetic.eventframework.event;

public interface ErrorIncomingEvent extends TrackedIncomingEvent {
    String getDescription();
    Throwable getThrowable();
}
