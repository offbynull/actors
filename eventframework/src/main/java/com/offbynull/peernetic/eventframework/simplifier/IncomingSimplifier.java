package com.offbynull.peernetic.eventframework.simplifier;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import java.util.Set;

public interface IncomingSimplifier<T extends IncomingEvent> {
    Set<Class<? extends IncomingEvent>> viewHandledEvents();
    SimplifierResult<T> simplify(IncomingEvent event);
}
