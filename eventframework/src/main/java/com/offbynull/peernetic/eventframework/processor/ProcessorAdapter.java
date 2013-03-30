package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils.OutputValue;

public abstract class ProcessorAdapter<R, N> implements Processor {

    private Processor processor;

    public ProcessorAdapter() {
    }

    public ProcessorAdapter(Processor processor) {
        if (processor == null) {
            throw new NullPointerException();
        }
        
        this.processor = processor;
    }

    protected final void setProcessor(Processor processor) {
        if (processor == null) {
            throw new NullPointerException();
        }
        
        if (this.processor != null) {
            throw new IllegalArgumentException();
        }
        
        this.processor = processor;
    }
    
    protected abstract N onResult(R res) throws Exception;
    
    protected abstract N onException(Exception e) throws Exception;
    
    @Override
    public final ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        ProcessResult pr;
        try {
            pr = processor.process(timestamp, event, trackedIdGen);
        } catch (Exception e) {
            N newRes = onException(e);
            return new FinishedProcessResult<>(newRes);
        }
        
        OutputValue<Boolean> successFlag = new OutputValue<>();
        @SuppressWarnings("unchecked")
        R res = (R) ProcessorUtils.extractFinishedResult(pr, successFlag);
        
        if (successFlag.getValue()) {
            N newRes = onResult(res);
            return new FinishedProcessResult<>(newRes);
        }
        
        return pr;
    }
}
