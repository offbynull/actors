package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.processors.NotifyProcessor.NotifyFailedException;
import com.offbynull.peernetic.chord.processors.QueryForFingerTableProcessor.QueryForFingerTableException;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils.OutputValue;

public final class StabilizeProcessor implements Processor {
    
    private QueryForPredecessorProcessor queryProcessor;
    private NotifyProcessor notifyProcessor;
    private Pointer base;
    private Pointer successor;
    private State state;
    private Pointer newSuccessor;

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
    
    private ProcessResult processInit(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        queryProcessor = new QueryForPredecessorProcessor(
                successor.getAddress());
        
        state = State.QUERY_RESPONSE_WAIT;
        
        return queryProcessor.process(timestamp, event, trackedIdGen);
    }
    
    private ProcessResult processQueryResponseWait(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        ProcessResult pr;
        try {
            pr = queryProcessor.process(timestamp, event, trackedIdGen);
        } catch (QueryForFingerTableException e) {
            throw new StabilizeFailedProcessorException();
        }
        
        OutputValue<Boolean> successfulExtraction = new OutputValue<>();
        Pointer predOfSuccessor = ProcessorUtils.extractFinishedResult(pr,
                successfulExtraction);
        
        if (successfulExtraction.getValue()) {
            if (predOfSuccessor == null) {
                newSuccessor = successor;
            } else {
                Id posId = predOfSuccessor.getId();
                Id baseId = base.getId();
                Id successorId = successor.getId();

                if (posId.isWithin(baseId, baseId, false, successorId, false)) {
                    successor = predOfSuccessor;
                } else {
                    newSuccessor = successor;
                }
            }
            
            state = State.NOTIFY_RESPONSE_WAIT;
            
            notifyProcessor = new NotifyProcessor(base, newSuccessor);

            pr = notifyProcessor.process(timestamp, event, trackedIdGen);
        }
        
        return pr;
    }
    
    private ProcessResult processNotifyResponseWait(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        ProcessResult pr;
        try {
            pr = notifyProcessor.process(timestamp, event, trackedIdGen);
        } catch (NotifyFailedException e) {
            throw new StabilizeFailedProcessorException();
        }
        
        if (pr instanceof FinishedProcessResult) {
            state = State.FINISHED;
            return new FinishedProcessResult<>(newSuccessor);
        }
        
        return pr;
    }

    private ProcessResult processFinished(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        throw new IllegalStateException();
    }
    
    public static final class StabilizeFailedProcessorException
            extends ProcessorException {
        
    }
    
    private enum State {
        INIT,
        QUERY_RESPONSE_WAIT,
        NOTIFY_RESPONSE_WAIT,
        FINISHED
    }
}
