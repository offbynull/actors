package com.offbynull.chord.processors;

import com.offbynull.chord.Address;
import com.offbynull.chord.FingerTable;
import com.offbynull.chord.Id;
import com.offbynull.chord.Pointer;
import com.offbynull.chord.messages.StatusRequest;
import com.offbynull.chord.messages.StatusResponse;
import com.offbynull.chord.messages.shared.NodeId;
import com.offbynull.chord.messages.shared.NodePointer;
import com.offbynull.chord.util.MessageUtils;
import com.offbynull.eventframework.handler.IncomingEvent;
import com.offbynull.eventframework.handler.OutgoingEvent;
import com.offbynull.eventframework.handler.TrackedIdGenerator;
import com.offbynull.eventframework.handler.TrackedUtil;
import com.offbynull.eventframework.handler.communication.ReceiveResponseIncomingEvent;
import com.offbynull.eventframework.handler.communication.Request;
import com.offbynull.eventframework.handler.communication.SendMessageOutgoingEvent;
import com.offbynull.eventframework.processor.FinishedProcessResult;
import com.offbynull.eventframework.processor.OngoingProcessResult;
import com.offbynull.eventframework.processor.ProcessResult;
import com.offbynull.eventframework.processor.Processor;
import com.offbynull.eventframework.processor.ProcessorException;
import java.util.Set;

public final class QueryProcessor implements Processor {
    
    private State state;
    private Address address;
    private long pendingId; 

    public QueryProcessor(TrackedIdGenerator tidGen, Address address) {
        if (tidGen == null || address == null) {
            throw new NullPointerException();
        }
        
        state = State.SEND;
        pendingId = tidGen.getNextId(); 
        this.address = address;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event) {
        switch (state) {
            case SEND: {
                return processSendState();
            }
            case RESPONSE_WAIT: {
                return processResponseWaitState(event);
            }
            case FINISHED: {
                return processFinishedState();
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    private ProcessResult processSendState() {
        state = State.RESPONSE_WAIT;

        Request req = new StatusRequest();
        OutgoingEvent outEvent = new SendMessageOutgoingEvent(req,
                address.getHost(), address.getPort(), pendingId);
        return new OngoingProcessResult(outEvent);
    }
    
    private ProcessResult processResponseWaitState(IncomingEvent inEvent) {
        TrackedUtil.throwProcessorExceptionOnError(inEvent, pendingId,
                QueryFailedProcessorException.class);
        
        ReceiveResponseIncomingEvent rrie = TrackedUtil.testAndConvert(inEvent,
                pendingId, ReceiveResponseIncomingEvent.class);
        
        if (rrie != null) {
            // got response
            StatusResponse resp = (StatusResponse) rrie.getResponse();
            NodeId nodeId = resp.getId();
            Set<NodePointer> nodePtrs = resp.getPointers();

            // reconstruct finger table
            FingerTable fingerTable;
            try {
                Id id = MessageUtils.convertTo(nodeId, false);
                Pointer ptr = new Pointer(id, address);
                fingerTable = new FingerTable(ptr);
                for (NodePointer pointer : nodePtrs) {
                    Pointer fingerPtr = MessageUtils.convertTo(pointer, false);
                    
                    fingerTable.put(fingerPtr);
                }
            } catch (RuntimeException re) {
                throw new QueryFailedProcessorException();
            }
            
            state = State.FINISHED;

            return new FinishedProcessResult(fingerTable);
        }
        
        return new OngoingProcessResult();
    }
    
    private ProcessResult processFinishedState() {
        throw new IllegalStateException();
    }
    
    private enum State {
        SEND,
        RESPONSE_WAIT,
        FINISHED
    }
    
    public static class QueryFailedProcessorException
            extends ProcessorException {
        
    }
}
