package com.offbynull.eventframework.processor;

import com.offbynull.eventframework.handler.OutgoingEvent;
import java.util.List;

public interface ProcessResult {
    List<OutgoingEvent> viewOutgoingEvents();
}
