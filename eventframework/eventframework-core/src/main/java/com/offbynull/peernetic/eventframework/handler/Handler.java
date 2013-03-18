package com.offbynull.peernetic.eventframework.handler;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import java.util.Set;

public interface Handler {
    Set<Class<? extends OutgoingEvent>> viewHandledEvents();
    OutgoingEventQueue start(IncomingEventQueue ieq);
    void stop();
}
