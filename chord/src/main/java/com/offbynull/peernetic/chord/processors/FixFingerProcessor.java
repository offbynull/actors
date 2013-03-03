package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.ChordState;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.RouteResult;
import com.offbynull.peernetic.chord.processors.QueryProcessor.QueryFailedProcessorException;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteProcessorException;
import com.offbynull.peernetic.eventframework.handler.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class FixFingerProcessor implements Processor {
    private TrackedIdGenerator tidGen;
    private ChordState chordState;
    private State state;
    private int index;
    private Pointer testPtr;
    private QueryProcessor queryProc;
    private RouteProcessor routeProc;

    public FixFingerProcessor(ChordState chordState, int index,
            TrackedIdGenerator tidGen) {
        if (tidGen == null || chordState == null) {
            throw new NullPointerException();
        }
        
        if (index < 0 || index >= chordState.getBitCount()) {
            throw new IllegalArgumentException();
        }
        
        this.tidGen = tidGen;
        this.chordState = chordState;
        this.state = State.TEST;
        this.index = index;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event) {
        switch (state) {
            case TEST:
                return processTestState(timestamp, event);
            case TEST_WAIT:
                return processTestWaitState(timestamp, event);
            case UPDATE_WAIT:
                return processUpdateWaitState(timestamp, event);
            case FINISHED:
                return processFinishedState(timestamp, event);
            default:
                throw new IllegalStateException();
        }
    }

    private ProcessResult processTestState(long timestamp,
            IncomingEvent event) {
        testPtr = chordState.getFinger(index);
        queryProc = new QueryProcessor(tidGen, testPtr.getAddress());
        ProcessResult queryProcRes = queryProc.process(timestamp, event);
        
        state = State.TEST_WAIT;
        
        return queryProcRes;
    }

    private ProcessResult processTestWaitState(long timestamp,
            IncomingEvent event) {
        ProcessResult queryProcRes;
        try {
            queryProcRes = queryProc.process(timestamp, event);
        } catch (QueryFailedProcessorException qfe) {
            // finger node failed to respond to test, so remove it before moving
            // on to update
            chordState.removeFinger(testPtr);
            return performUpdate(timestamp, event);
        }
        
        if (queryProcRes instanceof OngoingProcessResult) {
            // test hasn't finished
            return queryProcRes;
        }
        
        // finger node responded to test, move on to update
        return performUpdate(timestamp, event);
    }
    
    private ProcessResult performUpdate(long timestamp, IncomingEvent event) {
        Id destId = chordState.getExpectedFingerId(index);
        RouteResult routeRes = chordState.route(destId);
        
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
                state = State.FINISHED;
                return new FinishedProcessResult(false);
            }
            case SELF: {
                Id selfId = chordState.getBaseId();
                
                for (int i = index - 1; i >= 0; i++) {
                    Pointer ptr = chordState.getFinger(i);
                    if (!ptr.getId().equals(selfId)) {
                        bootstrap = ptr.getAddress();
                        break;
                    }
                }
                
                if (bootstrap == null) {
                    state = State.FINISHED;
                    return new FinishedProcessResult(false);
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
        
        Id selfId = chordState.getBaseId();
        routeProc = new RouteProcessor(tidGen, selfId, destId, bootstrap);
        state = State.UPDATE_WAIT;
        return routeProc.process(timestamp, event);
    }

    private ProcessResult processUpdateWaitState(long timestamp,
            IncomingEvent event) {
        ProcessResult routeProcRes;
        try {
            routeProcRes = routeProc.process(timestamp, event);
        } catch (RouteProcessorException rpe) {
            return new FinishedProcessResult(false);
        }
        
        if (routeProcRes instanceof OngoingProcessResult) {
            // route hasn't finished
            return routeProcRes;
        }

        // route has finished and finger's fixed
        return new FinishedProcessResult(true);
    }

    private ProcessResult processFinishedState(long timestamp,
            IncomingEvent event) {
        throw new IllegalStateException();
    }

    
    private enum State {
        TEST,
        TEST_WAIT,
        UPDATE_WAIT,
        FINISHED
    }
}
