package com.offbynull.eventframework.processor;

import com.offbynull.eventframework.handler.OutgoingEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class OngoingProcessResult implements ProcessResult {
    private List<OutgoingEvent> outgoingEvents;

    public OngoingProcessResult(OutgoingEvent ... outgoingEvents) {
        this(Arrays.<OutgoingEvent>asList(outgoingEvents));
    }

    public OngoingProcessResult(List<OutgoingEvent> outgoingEvents) {
        if (outgoingEvents == null || outgoingEvents.contains(null)) {
            throw new NullPointerException();
        } 
        this.outgoingEvents = Collections.unmodifiableList(
                new ArrayList<>(outgoingEvents));
    }
    
    public OngoingProcessResult(Set<OutgoingEvent> outgoingEvents) {
        if (outgoingEvents == null || outgoingEvents.contains(null)) {
            throw new NullPointerException();
        } 
        this.outgoingEvents = Collections.unmodifiableList(
                new ArrayList<>(outgoingEvents));
    }

    @Override
    public List<OutgoingEvent> viewOutgoingEvents() {
        return outgoingEvents;
    }
}
