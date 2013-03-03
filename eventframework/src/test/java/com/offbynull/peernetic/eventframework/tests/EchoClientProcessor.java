package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.handler.ErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.handler.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.handler.communication.ReceiveResponseIncomingEvent;
import com.offbynull.peernetic.eventframework.handler.communication.SendMessageOutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.lifecycle.InitializeIncomingEvent;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class EchoClientProcessor implements Processor {

    private State state;
    private TrackedIdGenerator idGenerator;
    private long trackedId;
    
    public EchoClientProcessor() {
        state = State.SEND;
        idGenerator = new TrackedIdGenerator();
    }
    
    @Override
    public ProcessResult process(long timestamp, IncomingEvent event) {
        if (event instanceof InitializeIncomingEvent && state == State.SEND) {
            trackedId = idGenerator.getNextId();
            OutgoingEvent outEvent = new SendMessageOutgoingEvent(
                    new FakeRequest("TEST 1 2 3"),
                    "localhost", 9111, trackedId);
            state = State.RECV;
            return new OngoingProcessResult(outEvent);
        } else if (event instanceof ReceiveResponseIncomingEvent && state == State.RECV) {
            state = State.DONE;
            ReceiveResponseIncomingEvent inEvent =
                    (ReceiveResponseIncomingEvent) event;
            FakeResponse fakeResponse = (FakeResponse) inEvent.getResponse();
            
            if (fakeResponse.getData().equals("TEST 1 2 3")) {
                return new FinishedProcessResult();
            } else {
                throw new IllegalStateException();
            }
        } else if (event instanceof ErrorIncomingEvent) {
            throw new IllegalStateException();
        }
        
        return new OngoingProcessResult();
    }
    
    private enum State {
        SEND,
        RECV,
        DONE
    }
}
