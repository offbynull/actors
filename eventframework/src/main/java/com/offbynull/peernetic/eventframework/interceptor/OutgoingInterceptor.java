package com.offbynull.peernetic.eventframework.interceptor;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.processor.Processor;
import java.util.Set;

public interface OutgoingInterceptor<T> {
    Set<Class<? extends OutgoingEvent>> viewHandledEvents();
    Processor intercept(long timestamp, OutgoingEvent event) throws Exception;
}
