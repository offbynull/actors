package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.processors.QueryForFingerTableProcessor.QueryForFingerTableException;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils;

public final class StabilizeProcessor implements Processor {
    
    private QueryForPredecessorProcessor queryProcessor;
    private NotifyProcessor notifyProcessor;
    private Pointer base;
    private Pointer successor;
    private State state;

    public StabilizeProcessor(Pointer base, Pointer successor) {
        if (base == null || successor == null) {
            throw new NullPointerException();
        }
        
        this.base = base;
        this.successor = successor;
        state = State.INIT;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        switch (state) {
            case INIT:
                return processInit(timestamp, event, trackedIdGen);
            case QUERY_RESPONSE_WAIT:
                return processQueryResponseWait(timestamp, event, trackedIdGen);
            case NOTIFY_RESPONSE_WAIT:
                return processNotifyResponseWait(timestamp, event,
                        trackedIdGen);
            case FINISHED:
                return processFinished(timestamp, event, trackedIdGen);
            default:
                throw new IllegalStateException();
        }
    }
    
    public ProcessResult processInit(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        queryProcessor = new QueryForPredecessorProcessor(
                successor.getAddress());
        
        state = State.QUERY_RESPONSE_WAIT;
        
        return queryProcessor.process(timestamp, event, trackedIdGen);
    }
    
    public ProcessResult processQueryResponseWait(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        ProcessResult pr;
        try {
            pr = queryProcessor.process(timestamp, event, trackedIdGen);
        } catch (QueryForFingerTableException e) {
            throw new StabilizeQueryFailedProcessorException();
        }
        
        // TODO result can be null, so this logic needs to be changed to move
        // to next state even if it is null
        Pointer predOfSuccessor = ProcessorUtils.extractFinishedResult(pr);
        if (predOfSuccessor != null) {
            Id posId = predOfSuccessor.getId();
            Id baseId = base.getId();
            Id successorId = successor.getId();
            
            if (posId.isWithin(baseId, baseId, false, successorId, false)) {
                successor = predOfSuccessor;
            }
            
            state = State.NOTIFY_RESPONSE_WAIT;
        }
        
        return pr;
    }
    
    public ProcessResult processNotifyResponseWait(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        throw new UnsupportedOperationException();
        
    }

    private ProcessResult processFinished(long timestamp, IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        throw new IllegalStateException();
    }
    
    public static class StabilizeFailedProcessorException
            extends ProcessorException {
        
    }
    
    public static final class StabilizeQueryFailedProcessorException
            extends StabilizeFailedProcessorException {
       
    }
    
    public static final class StabilizeNotifyFailedProcessorException
            extends StabilizeFailedProcessorException {
        
    }
    
    private enum State {
        INIT,
        QUERY_RESPONSE_WAIT,
        NOTIFY_RESPONSE_WAIT,
        FINISHED
    }
}
