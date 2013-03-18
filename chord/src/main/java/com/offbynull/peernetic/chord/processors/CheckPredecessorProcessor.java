package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.ChordState;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.processors.QueryProcessor.QueryFailedProcessorException;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class CheckPredecessorProcessor implements Processor {
    private ChordState chordState;
    private State state;
    private int index;
    private Pointer testPtr;
    private QueryProcessor queryProc;

    public CheckPredecessorProcessor(ChordState chordState, int index) {
        if (chordState == null) {
            throw new NullPointerException();
        }
        
        if (index < 0 || index >= chordState.getBitCount()) {
            throw new IllegalArgumentException();
        }
        
        this.chordState = chordState;
        this.state = State.TEST;
        this.index = index;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        switch (state) {
            case TEST:
                return processTestState(timestamp, event, trackedIdGen);
            case TEST_WAIT:
                return processTestWaitState(timestamp, event, trackedIdGen);
            case FINISHED:
                return processFinishedState(timestamp, event, trackedIdGen);
            default:
                throw new IllegalStateException();
        }
    }

    private ProcessResult processTestState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        testPtr = chordState.getFinger(index);
        queryProc = new QueryProcessor(testPtr.getAddress());
        ProcessResult queryProcRes = queryProc.process(timestamp, event,
                trackedIdGen);
        
        state = State.TEST_WAIT;
        
        return queryProcRes;
    }

    private ProcessResult processTestWaitState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        try {
            return queryProc.process(timestamp, event, trackedIdGen);
        } catch (QueryFailedProcessorException qfe) {
            chordState.setPredecessor(null);
            return new FinishedProcessResult();
        }
    }

    private ProcessResult processFinishedState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        throw new IllegalStateException();
    }

    
    private enum State {
        TEST,
        TEST_WAIT,
        FINISHED
    }
}
