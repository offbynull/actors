package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;

public final class StabilizeProcessor extends ProcessorChainAdapter<BitLimitedPointer> {

    private BitLimitedPointer base;
    private BitLimitedPointer successor;
    private BitLimitedPointer newSuccessor;
    
    public StabilizeProcessor(BitLimitedPointer base, BitLimitedPointer successor) {
        if (base == null || successor == null) {
            throw new NullPointerException();
        }
        
        this.base = base;
        this.successor = successor;
        
        setProcessor(new QueryForPredecessorProcessor(successor.getAddress()));
    }
    
    @Override
    protected NextAction onResult(Processor proc, Object res) throws Exception {
        if (proc instanceof QueryForPredecessorProcessor) {
            BitLimitedPointer predOfSuccessor = (BitLimitedPointer) res;
        
            if (predOfSuccessor == null) {
                newSuccessor = successor;
            } else {
                BitLimitedId posId = predOfSuccessor.getId();
                BitLimitedId baseId = base.getId();
                BitLimitedId successorId = successor.getId();

                if (posId.isWithin(baseId, baseId, false, successorId, false)) {
                    successor = predOfSuccessor;
                } else {
                    newSuccessor = successor;
                }
            }
            
            Processor newProc = new NotifyProcessor(base, newSuccessor);
            return new GoToNextProcessor(newProc);
        } else if (proc instanceof NotifyProcessor) {
            return new ReturnResult(newSuccessor);
        }
        
        throw new IllegalStateException();
    }

    @Override
    protected NextAction onException(Processor proc, Exception e)
            throws Exception {
        throw new StabilizeFailedException();        
    }
    
    public static final class StabilizeFailedException
            extends ProcessorException {
    }
}
