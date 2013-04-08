package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.RouteResult;
import com.offbynull.peernetic.chord.processors.QueryForFingerTableProcessor.QueryForFingerTableException;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteFailedException;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;

public final class FixFingerProcessor extends ProcessorChainAdapter<Boolean> {

    private FingerTable fingerTable;
    private int index;
    private Pointer testPtr;

    public FixFingerProcessor(FingerTable fingerTable, int index) {
        if (fingerTable == null) {
            throw new NullPointerException();
        }
        
        if (index < 0 || index >= fingerTable.getBaseId().getBitCount()) {
            throw new IllegalArgumentException();
        }
        
        this.fingerTable = new FingerTable(fingerTable);
        this.index = index;

        
        testPtr = fingerTable.get(index);
        Processor proc = new QueryForFingerTableProcessor(testPtr.getAddress());
        setProcessor(proc);
    }
    
    @Override
    protected NextAction onResult(Processor proc, Object res) throws Exception {
        if (proc instanceof QueryForFingerTableProcessor) {
            return performUpdate();
        } else if (proc instanceof RouteProcessor) {
            return new ReturnResult(true);
        }
        
        throw new IllegalStateException();
    }

    @Override
    protected NextAction onException(Processor proc, Exception e)
            throws Exception {
        if (e instanceof QueryForFingerTableException) {
            // finger node failed to respond to test, so remove it before moving
            // on to update
            fingerTable.remove(testPtr);
            return performUpdate();
        } else if (e instanceof RouteFailedException) {
            return new ReturnResult(false);
        } else if (e instanceof FixFingerFailedException) {
            throw e;
        }
        
        throw new FixFingerFailedException();
    }

    private NextAction performUpdate() throws Exception {
        Id destId = fingerTable.getExpectedId(index);
        RouteResult routeRes = fingerTable.route(destId);
        
        // If router result is FOUND, that means all other fingers are in front
        // or equal to what we're looking for. This would be the case if you
        // try to fix finger for finger[0] (also known as the successor). The
        // stabilize/notify process should help keep the successor in sync. End
        // the processor at this point.
        //
        // If route result is SELF, go back until you find a Id that isn't your
        // own, and use that to bootstrap the route address. Can't find one?
        // Then end the processor.
        //
        // Else, use the address from route res to bootstrap the route address
        Address bootstrap = null;
        switch (routeRes.getResultType()) {
            case FOUND: {
                return new ReturnResult(false);
            }
            case SELF: {
                Id selfId = fingerTable.getBaseId();
                
                for (int i = index - 1; i >= 0; i++) {
                    Pointer ptr = fingerTable.get(i);
                    if (!ptr.getId().equals(selfId)) {
                        bootstrap = ptr.getAddress();
                        break;
                    }
                }
                
                if (bootstrap == null) {
                    return new ReturnResult(false);
                }
                break;
            }
            case CLOSEST_PREDECESSOR: {
                bootstrap = routeRes.getPointer().getAddress();
                break;
            }
            default:
                throw new IllegalStateException();
        }
        
        Id selfId = fingerTable.getBaseId();
        Processor nextProc = new RouteProcessor(selfId, destId, bootstrap);
        return new GoToNextProcessor(nextProc);
    }

    public static final class FixFingerFailedException
        extends ProcessorException {
        
    }
}
