package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import java.util.List;

public interface ProcessResult {
    List<OutgoingEvent> viewOutgoingEvents();
}
