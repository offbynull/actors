package com.offbynull.peernetic.eventframework.interceptor;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.processor.Processor;
import java.util.List;
import java.util.Set;

public interface IncomingInterceptor<T> {
    Set<Class<? extends IncomingEvent>> viewHandledEvents();
    Processor intercept(long timestamp, IncomingEvent event,
            List<IncomingEvent> retInEvents, List<OutgoingEvent> retOutEvents)
            throws Exception;
}
