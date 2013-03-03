package com.offbynull.chord.processors;

import com.offbynull.chord.ChordState;
import com.offbynull.chord.Pointer;
import com.offbynull.chord.processors.QueryProcessor.QueryFailedProcessorException;
import com.offbynull.eventframework.handler.IncomingEvent;
import com.offbynull.eventframework.handler.TrackedIdGenerator;
import com.offbynull.eventframework.processor.FinishedProcessResult;
import com.offbynull.eventframework.processor.ProcessResult;
import com.offbynull.eventframework.processor.Processor;

public final class CheckPredeccesorProcessor implements Processor {
    private TrackedIdGenerator tidGen;
    private ChordState chordState;
    private State state;
    private int index;
    private Pointer testPtr;
    private QueryProcessor queryProc;

    public CheckPredeccesorProcessor(ChordState chordState, int index,
            TrackedIdGenerator tidGen) {
        if (tidGen == null || chordState == null) {
            throw new NullPointerException();
        }
        
        if (index < 0 || index >= chordState.getBitCount()) {
            throw new IllegalArgumentException();
        }
        
        this.tidGen = tidGen;
        this.chordState = chordState;
        this.state = State.TEST;
        this.index = index;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event) {
        switch (state) {
            case TEST:
                return processTestState(timestamp, event);
            case TEST_WAIT:
                return processTestWaitState(timestamp, event);
            case FINISHED:
                return processFinishedState(timestamp, event);
            default:
                throw new IllegalStateException();
        }
    }

    private ProcessResult processTestState(long timestamp,
            IncomingEvent event) {
        testPtr = chordState.getFinger(index);
        queryProc = new QueryProcessor(tidGen, testPtr.getAddress());
        ProcessResult queryProcRes = queryProc.process(timestamp, event);
        
        state = State.TEST_WAIT;
        
        return queryProcRes;
    }

    private ProcessResult processTestWaitState(long timestamp,
            IncomingEvent event) {
        try {
            return queryProc.process(timestamp, event);
        } catch (QueryFailedProcessorException qfe) {
            chordState.setPredeccesor(null);
            return new FinishedProcessResult();
        }
    }

    private ProcessResult processFinishedState(long timestamp,
            IncomingEvent event) {
        throw new IllegalStateException();
    }

    
    private enum State {
        TEST,
        TEST_WAIT,
        FINISHED
    }
}
