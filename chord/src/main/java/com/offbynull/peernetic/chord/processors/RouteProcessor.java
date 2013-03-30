package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.RouteResult;
import com.offbynull.peernetic.chord.processors.RouteProcessor.Result;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class RouteProcessor extends ProcessorChainAdapter<Result> {

    private Id findId;
    private Id selfId;
    private Id lastHitId;
    private Address nextSearchAddress;
    private Set<Address> accessedAddresses;

    public RouteProcessor(Id selfId, Id findId, Address bootstrap) {
        if (findId == null || selfId == null || bootstrap == null) {
            throw new NullPointerException();
        }
        this.findId = findId;
        this.selfId = selfId;
        this.nextSearchAddress = bootstrap;
        accessedAddresses = new HashSet<>();
        
        accessedAddresses.add(bootstrap);
        Processor proc = new QueryForFingerTableProcessor(bootstrap);
        setProcessor(proc);
    }
    
    @Override
    protected NextAction onResult(Processor proc, Object res) throws Exception {
        if (proc instanceof QueryForFingerTableProcessor) {
            FingerTable ft = (FingerTable) res;
            RouteResult routeRes = ft.route(findId);
            Pointer ptr = routeRes.getPointer();
            Id ptrId = ptr.getId();

            if (ptrId.equals(selfId)) {
                throw new RouteFailedSelfException();
            }
            
            if (lastHitId != null
                    && ptrId.comparePosition(lastHitId, lastHitId) <= 0) {
                throw new RouteFailedBackwardException();
            }
            
            lastHitId = ptrId;
            
            accessedAddresses.add(nextSearchAddress);
            
            switch (routeRes.getResultType()) {
                case FOUND:
                case SELF: {
                    Result action = new Result(accessedAddresses, ptr);
                    return new ReturnResult(action);
                }
                case CLOSEST_PREDECESSOR: {
                    nextSearchAddress = ptr.getAddress();
                    Processor nextProc = new QueryForFingerTableProcessor(
                            nextSearchAddress);
                    return new GoToNextProcessor(nextProc);
                }
                default:
                    throw new IllegalStateException();
            }
        }
        
        throw new IllegalStateException();
    }

    @Override
    protected NextAction onException(Processor proc, Exception e)
            throws Exception {
        if (e instanceof RouteFailedException) {
            throw e;
        }
        
        throw new RouteFailedException();
    }

    public static class RouteFailedException
        extends ProcessorException {
        
    }

    public static final class RouteFailedSelfException
            extends RouteFailedException {
        
    }
    
    public static final class RouteFailedBackwardException
            extends RouteFailedException {
        
    }
    
    public static final class Result {
        private Set<Address> accessedAddresses;
        private Pointer found;

        private Result(Set<Address> accessedAddresses, Pointer found) {
            // no need to check for null here to make backing copies, only
            // routeprocessor calls and has access to this, and its lifecycle
            // ends as soon as it creates one of these.
            this.accessedAddresses = accessedAddresses;
            this.found = found;
        }

        public Set<Address> viewAccessedAddresses() {
            return Collections.unmodifiableSet(accessedAddresses);
        }

        public Pointer getFound() {
            return found;
        }
    }
}
