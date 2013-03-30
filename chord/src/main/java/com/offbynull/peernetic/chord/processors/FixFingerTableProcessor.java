package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.ChordState;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class FixFingerTableProcessor implements Processor {

    private ChordState chordState;
    private int index;
    private FixFingerProcessor fixFingerProc;
    private State state;

    public FixFingerTableProcessor(ChordState chordState) {
        if (chordState == null) {
            throw new NullPointerException();
        }
        
        this.chordState = chordState;
    }
    
    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        switch (state) {
            case INIT:
                return processInitState(timestamp, event, trackedIdGen);
            case RUNNING:
            case STALLING:
                return processRunningState(timestamp, event, trackedIdGen);
            default:
                throw new IllegalStateException();
        }
    }

    private ProcessResult processInitState(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        if (chordState.getBaseId().getBitCount() == 1) {
            // The finger table only contains the successor, which is updated
            // elsewhere. Do nothing now and force the processor to do nothing
            // for ever.
            state = State.STALLING;
            return new OngoingProcessResult();
        }
        
        index = 1;
        fixFingerProc = new FixFingerProcessor(chordState, index);
        fixFingerProc.process(timestamp, event, trackedIdGen);
        state = State.RUNNING;
        return new OngoingProcessResult();
    }

    private ProcessResult processRunningState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        ProcessResult pr = fixFingerProc.process(timestamp, event,
                trackedIdGen);
        
        if (pr instanceof FinishedProcessResult) {
            index++;
            
            if (index == chordState.getBaseId().getBitCount()) {
                index = 1;
            }
            
            fixFingerProc = new FixFingerProcessor(chordState, index);
            fixFingerProc.process(timestamp, event, trackedIdGen);
        }
        
        return new OngoingProcessResult();
    }
    
    private enum State {
        INIT,
        RUNNING,
        STALLING
    }
}
