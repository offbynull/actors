package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.ChordState;
import com.offbynull.peernetic.eventframework.handler.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class FixFingerTableProcessor implements Processor {

    private TrackedIdGenerator tidGen;
    private ChordState chordState;
    private int index;
    private FixFingerProcessor fixFingerProc;
    private State state;

    public FixFingerTableProcessor(ChordState chordState,
            TrackedIdGenerator tidGen) {
        if (tidGen == null || chordState == null) {
            throw new NullPointerException();
        }
        
        this.tidGen = tidGen;
        this.chordState = chordState;
    }
    
    @Override
    public ProcessResult process(long timestamp, IncomingEvent event) {
        switch (state) {
            case INIT:
                return processInitState(timestamp, event);
            case RUNNING:
                return processRunningState(timestamp, event);
            case STALLING:
                return processRunningState(timestamp, event);
            default:
                throw new IllegalStateException();
        }
    }

    private ProcessResult processInitState(long timestamp, IncomingEvent event) {
        if (chordState.getBaseId().getBitCount() == 1) {
            // The finger table only contains the successor, which is updated
            // elsewhere. Do nothing now and force the processor to do nothing
            // for ever.
            state = State.STALLING;
            return new OngoingProcessResult();
        }
        
        index = 1;
        fixFingerProc = new FixFingerProcessor(chordState, index, tidGen);
        fixFingerProc.process(timestamp, event);
        state = State.RUNNING;
        return new OngoingProcessResult();
    }

    private ProcessResult processRunningState(long timestamp, IncomingEvent event) {
        ProcessResult pr = fixFingerProc.process(timestamp, event);
        
        if (pr instanceof FinishedProcessResult) {
            index++;
            
            if (index == chordState.getBaseId().getBitCount()) {
                index = 1;
            }
            
            fixFingerProc = new FixFingerProcessor(chordState, index, tidGen);
            fixFingerProc.process(timestamp, event);
        }
        
        return new OngoingProcessResult();
    }
    
    private enum State {
        INIT,
        RUNNING,
        STALLING
    }
}
