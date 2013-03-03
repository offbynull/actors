package com.offbynull.eventframework.handler;

public interface ErrorIncomingEvent extends TrackedIncomingEvent {
    String getDescription();
    Throwable getThrowable();
}
