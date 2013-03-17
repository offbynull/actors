package com.offbynull.peernetic.eventframework.simplifier;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class SimplifierResult<T extends IncomingEvent> {
    private Processor<T> newProcessor;
    private boolean consumeEvent;

    public SimplifierResult(Processor<T> newProcessor) {
        this(newProcessor, false);
    }

    public SimplifierResult(Processor<T> newProcessor, boolean consumeEvent) {
        if (newProcessor == null) {
            throw new NullPointerException();
        }
        
        this.newProcessor = newProcessor;
        this.consumeEvent = consumeEvent;
    }

    public Processor<T> getNewProcessor() {
        return newProcessor;
    }

    public boolean isConsumeEvent() {
        return consumeEvent;
    }
    
}
