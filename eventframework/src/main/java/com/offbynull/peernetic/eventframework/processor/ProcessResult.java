package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.handler.OutgoingEvent;
import java.util.List;

public interface ProcessResult<T> {
    List<OutgoingEvent> viewOutgoingEvents();
}
