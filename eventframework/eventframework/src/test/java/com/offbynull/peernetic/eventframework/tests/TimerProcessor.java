package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.impl.basic.lifecycle.InitializeIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.basic.timer.NewTimerOutgoingEvent;
import com.offbynull.peernetic.eventframework.impl.basic.timer.TimerHitIncomingEvent;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class TimerProcessor implements Processor {
    
    private State state;
    private long duration;
    
    public TimerProcessor(long duration) {
        if (duration < 1L) {
            throw new IllegalArgumentException();
        }
        state = State.START;
        this.duration = duration;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        if (event instanceof InitializeIncomingEvent && state == State.START) {
            state = State.WAIT;
            OutgoingEvent outEvent = new NewTimerOutgoingEvent(
                    trackedIdGen.getNextId(), duration);
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
