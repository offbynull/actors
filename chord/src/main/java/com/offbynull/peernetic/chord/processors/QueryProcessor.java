package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveResponseIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.message.Request;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.util.MessageUtils;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.event.EventUtils;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import java.util.Set;

public final class QueryProcessor implements Processor<FingerTable> {
    
    private State state;
    private Address address;
    private long pendingId; 

    public QueryProcessor(Address address) {
        if (address == null) {
            throw new NullPointerException();
        }
        
        state = State.SEND;
        this.address = address;
    }

    @Override
    public ProcessResult<FingerTable> process(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        switch (state) {
            case SEND: {
                return processSendState(trackedIdGen);
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
    
    private ProcessResult<FingerTable> processSendState(
            TrackedIdGenerator trackedIdGen) {
        pendingId = trackedIdGen.getNextId();
        
        state = State.RESPONSE_WAIT;

        Request req = new StatusRequest();
        OutgoingEvent outEvent = new SendMessageOutgoingEvent(req,
                address.getHost(), address.getPort(), pendingId);
        return new OngoingProcessResult<>(outEvent);
    }
    
    private ProcessResult<FingerTable> processResponseWaitState(
            IncomingEvent inEvent) {
        EventUtils.throwProcessorExceptionOnError(inEvent, pendingId,
                QueryFailedProcessorException.class);
        
        ReceiveResponseIncomingEvent rrie = EventUtils.testAndConvert(inEvent,
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

            return new FinishedProcessResult<>(fingerTable);
        }
        
        return new OngoingProcessResult<>();
    }
    
    private ProcessResult<FingerTable> processFinishedState() {
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
