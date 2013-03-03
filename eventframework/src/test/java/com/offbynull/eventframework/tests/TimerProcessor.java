package com.offbynull.eventframework.tests;

import com.offbynull.eventframework.handler.IncomingEvent;
import com.offbynull.eventframework.handler.OutgoingEvent;
import com.offbynull.eventframework.handler.TrackedIdGenerator;
import com.offbynull.eventframework.handler.lifecycle.InitializeIncomingEvent;
import com.offbynull.eventframework.handler.timer.NewTimerOutgoingEvent;
import com.offbynull.eventframework.handler.timer.TimerHitIncomingEvent;
import com.offbynull.eventframework.processor.FinishedProcessResult;
import com.offbynull.eventframework.processor.OngoingProcessResult;
import com.offbynull.eventframework.processor.ProcessResult;
import com.offbynull.eventframework.processor.Processor;

public final class TimerProcessor implements Processor {
    
    private State state;
    private long duration;
    private TrackedIdGenerator idGenerator;
    
    public TimerProcessor(long duration) {
        if (duration < 1L) {
            throw new IllegalArgumentException();
        }
        state = State.START;
        idGenerator = new TrackedIdGenerator();
        this.duration = duration;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event) {
        if (event instanceof InitializeIncomingEvent && state == State.START) {
            state = State.WAIT;
            OutgoingEvent outEvent = new NewTimerOutgoingEvent(
                    idGenerator.getNextId(), duration);
            return new OngoingProcessResult(outEvent);
        }

        if (event instanceof TimerHitIncomingEvent && state == State.WAIT) {
            state = State.STOP;
            return new FinishedProcessResult();
        }
        
        throw new IllegalStateException();
    }
    
    private enum State {
        START,
        WAIT,
        STOP
    }
}
