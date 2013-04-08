package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.ChordState;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;

public final class FixFingerTableProcessor
        extends ProcessorChainAdapter<Boolean> {

    private ChordState chordState;
    private int index;
    
    public FixFingerTableProcessor(ChordState chordState) {
        if (chordState == null) {
            throw new NullPointerException();
        }

        if (chordState.getBaseId().getBitCount() == 1) {
            // The finger table only contains the successor, which is updated
            // elsewhere. Do nothing now and force the processor to do nothing
            // for ever.
            throw new IllegalArgumentException();
        }
        
        this.chordState = chordState;
        this.index = 1;
        
        Processor proc = new FixFingerProcessor(chordState, index);
        setProcessor(proc);
    }
    
    @Override
    protected NextAction onResult(Processor proc, Object res)
            throws Exception {
        index++;
        
        if (index == chordState.getBitCount()) {
            return new ReturnResult(true);
        }
        
        Processor newProc = new FixFingerProcessor(chordState, index);
        return new GoToNextProcessor(newProc);
    }

    @Override
    protected NextAction onException(Processor proc, Exception e)
            throws Exception {
        if (e instanceof FixFingerTableFailedException) {
            throw e;
        }
        
        throw new FixFingerTableFailedException();
    }
    
    public static final class FixFingerTableFailedException
        extends ProcessorException {
        
    }
}
