package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils;

public final class StabilizeProcessor extends ProcessorChainAdapter<Pointer> {

    private Pointer base;
    private Pointer successor;
    private Pointer newSuccessor;
    
    public StabilizeProcessor(Pointer base, Pointer successor) {
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
            Pointer predOfSuccessor = (Pointer) res;
        
            if (predOfSuccessor == null) {
                newSuccessor = successor;
            } else {
                Id posId = predOfSuccessor.getId();
                Id baseId = base.getId();
                Id successorId = successor.getId();

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
