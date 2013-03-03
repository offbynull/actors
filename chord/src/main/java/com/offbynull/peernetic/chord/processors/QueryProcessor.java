package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.util.MessageUtils;
import com.offbynull.peernetic.eventframework.handler.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.handler.TrackedUtil;
import com.offbynull.peernetic.eventframework.handler.communication.ReceiveResponseIncomingEvent;
import com.offbynull.peernetic.eventframework.handler.communication.Request;
import com.offbynull.peernetic.eventframework.handler.communication.SendMessageOutgoingEvent;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
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
