package com.offbynull.peernetic.eventframework.handler;

public interface ErrorIncomingEvent extends TrackedIncomingEvent {
    String getDescription();
    Throwable getThrowable();
}
