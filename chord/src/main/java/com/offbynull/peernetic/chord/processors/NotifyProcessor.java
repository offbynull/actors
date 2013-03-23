package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.SetPredecessorRequest;
import com.offbynull.peernetic.chord.messages.SetPredecessorResponse;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.util.MessageUtils;
import com.offbynull.peernetic.eventframework.event.EventUtils;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.NetEventUtils;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;

public final class NotifyProcessor implements Processor<Boolean> {
    
    private long netPendingId;
    private State state;
    private Pointer base;
    private Pointer dest;

    public NotifyProcessor(Pointer base, Pointer dest) {
        if (base == null || dest == null) {
            throw new NullPointerException();
        }
        
        this.base = base;
        this.dest = dest;
    }
    
    
    @Override
    public ProcessResult<Boolean> process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        switch (state) {
            case INIT:
                return processInit(timestamp, event, trackedIdGen);
            case WAIT_RESPONSE:
                return processWaitResponse(timestamp, event, trackedIdGen);
            case FINISHED:
                return processFinished(timestamp, event, trackedIdGen);
            default:
                throw new IllegalStateException();
        }
    }
    
    private ProcessResult<Boolean> processInit(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        netPendingId = trackedIdGen.getNextId();

        Address destAddr = dest.getAddress();

        NodePointer baseNp = MessageUtils.createFrom(base, false);

        SetPredecessorRequest req = new SetPredecessorRequest();
        req.setPredecessor(baseNp);

        OutgoingEvent outEvent = new SendMessageOutgoingEvent(req,
                destAddr.getHost(), destAddr.getPort(), netPendingId);

        state = State.WAIT_RESPONSE;

        return new OngoingProcessResult<>(outEvent);
    }
    
    private ProcessResult<Boolean> processWaitResponse(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        EventUtils.throwProcessorExceptionOnError(event, netPendingId,
                NotifyFailedProcessorException.class);
        
        SetPredecessorResponse resp = NetEventUtils.testAndConvertResponse(
                event, netPendingId);
        
        if (resp != null) {
            NodePointer otherPredNp = resp.getAssignedPredecessor();        
            Pointer otherPred = MessageUtils.convertTo(otherPredNp, false);
            boolean assigned = base.equals(otherPred);
            return new FinishedProcessResult<>(assigned);
        }
        
        return new OngoingProcessResult<>();
    }
    
    private ProcessResult<Boolean> processFinished(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        throw new IllegalStateException();
    }
    
    public static class NotifyFailedProcessorException
            extends ProcessorException {
        
    }
    
    private enum State {
        INIT,
        WAIT_RESPONSE,
        FINISHED
    }
}
