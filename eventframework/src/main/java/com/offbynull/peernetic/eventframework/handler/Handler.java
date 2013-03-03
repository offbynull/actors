package com.offbynull.peernetic.eventframework.handler;

import java.util.Set;

public interface Handler {
    Set<Class<? extends OutgoingEvent>> viewHandledEvents();
    OutgoingEventQueue start(IncomingEventQueue ieq);
    void stop();
}
