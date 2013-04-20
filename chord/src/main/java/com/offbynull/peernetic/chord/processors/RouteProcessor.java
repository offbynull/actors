package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteProcessorResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.p2ptools.identification.Address;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import com.offbynull.peernetic.p2ptools.overlay.structured.chord.FingerTable;
import com.offbynull.peernetic.p2ptools.overlay.structured.chord.FingerTable.RouteResult;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class RouteProcessor extends ProcessorChainAdapter<RouteProcessorResult> {

    private BitLimitedId findId;
    private BitLimitedId selfId;
    private BitLimitedId lastHitId;
    private Address nextSearchAddress;
    private Set<Address> accessedAddresses;

    public RouteProcessor(BitLimitedId selfId, BitLimitedId findId, Address bootstrap) {
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
            BitLimitedPointer ptr = routeRes.getPointer();
            BitLimitedId ptrId = ptr.getId();

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
                    RouteProcessorResult action =
                            new RouteProcessorResult(accessedAddresses, ptr);
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
    
    public static final class RouteProcessorResult {
        private Set<Address> accessedAddresses;
        private BitLimitedPointer found;

        private RouteProcessorResult(Set<Address> accessedAddresses, BitLimitedPointer found) {
            // no need to check for null here to make backing copies, only
            // routeprocessor calls and has access to this, and its lifecycle
            // ends as soon as it creates one of these.
            this.accessedAddresses = accessedAddresses;
            this.found = found;
        }

        public Set<Address> viewAccessedAddresses() {
            return Collections.unmodifiableSet(accessedAddresses);
        }

        public BitLimitedPointer getFound() {
            return found;
        }
    }
}
