package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.event.ErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.SuccessIncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.handler.communication.ReceiveMessageIncomingEvent;
import com.offbynull.peernetic.eventframework.handler.communication.SendResponseOutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.communication.StartServerOutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.communication.StopServerOutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.lifecycle.InitializeIncomingEvent;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import java.util.ArrayList;
import java.util.List;

public final class EchoServerProcessor implements Processor {

    private State state;
    private TrackedIdGenerator idGenerator;
    private long trackedId;
    private int msgCounter;
    private int msgCountTillFinish;
    
    public EchoServerProcessor(int msgCountTillFinish) {
        if (msgCountTillFinish < 1) {
            throw new IllegalArgumentException();
        }
        state = State.NONE;
        idGenerator = new TrackedIdGenerator();
        this.msgCountTillFinish = msgCountTillFinish;
    }
    
    @Override
    public ProcessResult process(long timestamp, IncomingEvent event) {
        if (event instanceof InitializeIncomingEvent && state == State.NONE) {
            state = State.STARTING;
            trackedId = idGenerator.getNextId();
            OutgoingEvent outEvent = new StartServerOutgoingEvent(9111,
                    trackedId);
            return new OngoingProcessResult(outEvent);
        } else if (event instanceof SuccessIncomingEvent && state == State.STARTING) {
            state = State.STARTED;
            return new OngoingProcessResult();
        } else if (event instanceof ReceiveMessageIncomingEvent && state == State.STARTED) {
            List<OutgoingEvent> outEvents = new ArrayList<>();
            
            ReceiveMessageIncomingEvent inEvent =
                    (ReceiveMessageIncomingEvent) event;
            FakeRequest fakeRequest = (FakeRequest) inEvent.getRequest();
            OutgoingEvent respEvent = new SendResponseOutgoingEvent(
                    new FakeResponse(fakeRequest.getData()),
                    inEvent.getPendingId(), trackedId);
            outEvents.add(respEvent);
            
            msgCounter++;
            
            if (msgCountTillFinish == msgCounter) {
                OutgoingEvent stopEvent = new StopServerOutgoingEvent(trackedId);
                outEvents.add(stopEvent);
                state = State.STOPPING;
            }
            
            return new OngoingProcessResult(outEvents);
        } else if (event instanceof SuccessIncomingEvent && state == State.STOPPING) {
            state = State.STOPPED;
            return new FinishedProcessResult();
        } else if (event instanceof ErrorIncomingEvent) {
            throw new IllegalStateException();
        }

        return new OngoingProcessResult();
    }

    private enum State {

        NONE,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED
    }
}
