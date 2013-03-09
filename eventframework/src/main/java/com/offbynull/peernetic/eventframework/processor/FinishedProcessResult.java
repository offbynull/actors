package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.handler.OutgoingEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class FinishedProcessResult<T> implements ProcessResult<T> {
    private T result;
    private List<OutgoingEvent> outgoingEvents;

    public FinishedProcessResult(OutgoingEvent ... outgoingEvents) {
        this(null, Arrays.<OutgoingEvent>asList(outgoingEvents));
    }

    public FinishedProcessResult(List<OutgoingEvent> outgoingEvents) {
        this(null, outgoingEvents);
    }
    
    public FinishedProcessResult(Set<OutgoingEvent> outgoingEvents) {
        this(null, outgoingEvents);
    }
    
    public FinishedProcessResult(T result, OutgoingEvent ... outgoingEvents) {
        this(result, Arrays.<OutgoingEvent>asList(outgoingEvents));
    }

    public FinishedProcessResult(T result, List<OutgoingEvent> outgoingEvents) {
        if (outgoingEvents == null || outgoingEvents.contains(null)) {
            throw new NullPointerException();
        } 
        this.outgoingEvents = Collections.unmodifiableList(
                new ArrayList<>(outgoingEvents));
        this.result = result;
    }
    
    public FinishedProcessResult(T result, Set<OutgoingEvent> outgoingEvents) {
        if (outgoingEvents == null || outgoingEvents.contains(null)) {
            throw new NullPointerException();
        } 
        this.outgoingEvents = Collections.unmodifiableList(
                new ArrayList<>(outgoingEvents));
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

    @Override
    public List<OutgoingEvent> viewOutgoingEvents() {
        return outgoingEvents;
    }
}
