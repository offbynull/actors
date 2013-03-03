package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.handler.IncomingEvent;

public interface Processor {
    ProcessResult process(long timestamp, IncomingEvent event) throws Exception;
}
