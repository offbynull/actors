package com.offbynull.peernetic.eventframework.simplifier;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import java.util.Set;

public interface OutgoingSimplifier<T extends IncomingEvent> {
    Set<Class<? extends OutgoingEvent>> viewHandledEvents();
    SimplifierResult<T> simplify(OutgoingEvent event);
}
