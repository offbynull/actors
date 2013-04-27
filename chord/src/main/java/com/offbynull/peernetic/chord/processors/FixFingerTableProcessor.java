package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.p2ptools.overlay.chord.FingerTable;

public final class FixFingerTableProcessor
        extends ProcessorChainAdapter<FingerTable> {

    private FingerTable fingerTable;
    private int index;
    private int bitCount;
    
    public FixFingerTableProcessor(FingerTable fingerTable) {
        if (fingerTable == null) {
            throw new NullPointerException();
        }

        bitCount = fingerTable.getBaseId().getBitCount();
        
        if (bitCount == 1) {
            // The finger table only contains the successor, which is updated
            // elsewhere. Do nothing now and force the processor to do nothing
            // for ever.
            throw new IllegalArgumentException();
        }
        
        this.fingerTable = new FingerTable(fingerTable);
        this.index = 1;
        
        Processor proc = new FixFingerProcessor(fingerTable, index);
        setProcessor(proc);
    }
    
    @Override
    protected NextAction onResult(Processor proc, Object res)
            throws Exception {
        index++;
        
        if (index == bitCount) {
            return new ReturnResult(fingerTable);
        }
        
        Processor newProc = new FixFingerProcessor(fingerTable, index);
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
