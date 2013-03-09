package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.handler.IncomingEvent;

public interface Processor<T> {
    ProcessResult<T> process(long timestamp, IncomingEvent event) throws Exception;
}
