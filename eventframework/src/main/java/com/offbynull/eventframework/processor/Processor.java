package com.offbynull.eventframework.processor;

import com.offbynull.eventframework.handler.IncomingEvent;

public interface Processor {
    ProcessResult process(long timestamp, IncomingEvent event) throws Exception;
}
