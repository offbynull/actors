package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.RouteResult;
import com.offbynull.peernetic.chord.processors.QueryProcessor.QueryFailedProcessorException;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RouteProcessor implements Processor {
    private Id findId;
    private Id selfId;
    private Id lastHitId;
    private Address nextSearchAddress;
    private State state;
    private QueryProcessor queryProc;
    private Set<Address> accessedAddresses;

    public RouteProcessor(Id selfId, Id findId, Address bootstrap) {
        if (findId == null || selfId == null || bootstrap == null) {
            throw new NullPointerException();
        }
        this.findId = findId;
        this.selfId = selfId;
        this.nextSearchAddress = bootstrap;
        accessedAddresses = new HashSet<>();
        
        state = State.INIT;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        switch (state) {
            case INIT:
                return processInitState(timestamp, event, trackedIdGen);
            case PROCESSING:
                return processProcessState(timestamp, event, trackedIdGen);
            case FINISHED:
                return processFinishedState();
            default:
                throw new IllegalStateException();
        }
    }
    
    private ProcessResult processInitState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        List<OutgoingEvent> outEvents = startNewQuery(timestamp, event,
                trackedIdGen);
        state = State.PROCESSING;
        return new OngoingProcessResult(outEvents);
    }
    
    private ProcessResult processProcessState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        ProcessResult queryRes;
        try {
            queryRes = queryProc.process(timestamp, event, trackedIdGen);
        } catch (QueryFailedProcessorException qfpe) {
            throw new RouteFailedProcessorException();
        }
        
        if (queryRes instanceof FinishedProcessResult) {
            FinishedProcessResult queryFinRes =
                    (FinishedProcessResult) queryRes;
            
            FingerTable ft = (FingerTable) queryFinRes.getResult();
            RouteResult routeRes = ft.route(findId);
            Pointer ptr = routeRes.getPointer();
            Id ptrId = ptr.getId();

            if (ptrId.equals(selfId)) {
                throw new RouteSelfProcessorException();
            }
            
            if (lastHitId != null
                    && ptrId.comparePosition(lastHitId, lastHitId) <= 0) {
                throw new RouteBackwardProcessorException();
            }
            
            lastHitId = ptrId;
            
            accessedAddresses.add(nextSearchAddress);
            
            switch (routeRes.getResultType()) {
                case FOUND:
                case SELF: {
                    Result result = new Result(accessedAddresses, ptr);
                    return new FinishedProcessResult(result);
                }
                case CLOSEST_PREDECESSOR: {
                    nextSearchAddress = ptr.getAddress();
                    
                    List<OutgoingEvent> outEvents = startNewQuery(timestamp,
                            event, trackedIdGen);
                    
                    return new OngoingProcessResult(outEvents);
                }
                default:
                    throw new IllegalStateException();
            }
        }
        
        return new OngoingProcessResult();
    }
    
    private ProcessResult processFinishedState() {
        throw new IllegalStateException();
    }
    
    private List<OutgoingEvent> startNewQuery(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        queryProc = new QueryProcessor(nextSearchAddress);
        ProcessResult pr = queryProc.process(timestamp, event, trackedIdGen);
        
        return pr.viewOutgoingEvents();
    }
    
    private enum State {
        INIT,
        PROCESSING,
        FINISHED
    }
    
    public static class RouteProcessorException
        extends ProcessorException {
        
    }
    
    public static final class RouteFailedProcessorException
            extends RouteProcessorException {
        
    }

    public static final class RouteSelfProcessorException
            extends RouteProcessorException {
        
    }
    
    public static final class RouteBackwardProcessorException
            extends RouteProcessorException {
        
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
