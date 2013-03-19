package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;

public interface Processor<T> {
    ProcessResult<T> process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception;
}
