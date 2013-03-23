package com.offbynull.peernetic.eventframework.impl.network.simpletcp;

import com.google.common.collect.Sets;
import com.offbynull.peernetic.eventframework.event.EventUtils;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.impl.network.message.Request;
import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import java.util.Set;

public final class SendMessageProcessor implements Processor {
    private State state;
    private String address;
    private int port;
    private long pendingId; 
    private Request request;
    private Set<Class<? extends Response>> responseTypes;

    public SendMessageProcessor(String address, int port, Request request,
            Class<? extends Response> ... responseTypes) {
        this(address, port, request, Sets.newHashSet(responseTypes));
    }
    
    public SendMessageProcessor(String address, int port, Request request,
            Set<Class<? extends Response>> responseTypes) {
        if (address == null || request == null || responseTypes == null
                || responseTypes.contains(null)) {
            throw new NullPointerException();
        }
        
        if (port < 0 || port > 65535) {
            throw new NullPointerException();
        }
        
        state = State.SEND;
        this.address = address;
        this.port = port;
        this.request = request;
        this.responseTypes = responseTypes;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
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
    
    private ProcessResult processSendState(TrackedIdGenerator trackedIdGen) {
        pendingId = trackedIdGen.getNextId();
        
        state = State.RESPONSE_WAIT;

        OutgoingEvent outEvent = new SendMessageOutgoingEvent(request, address,
                port, pendingId);
        return new OngoingProcessResult(outEvent);
    }
    
    private ProcessResult processResponseWaitState(IncomingEvent inEvent) {
        EventUtils.throwProcessorExceptionOnError(inEvent, pendingId,
                SendMessageException.class);
        
        Response resp = NetEventUtils.testAndConvertResponse(inEvent,
                pendingId);
        
        if (resp != null) {
            if (!responseTypes.contains(resp.getClass())) {
                throw new SendMessageException();
            }
            
            state = State.FINISHED;

            return new FinishedProcessResult<>(resp);
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
    
    public static class SendMessageException
            extends ProcessorException {
        
    }
}
