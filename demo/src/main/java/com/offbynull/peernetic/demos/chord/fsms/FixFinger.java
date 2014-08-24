package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.demos.chord.ChordContext;
import com.offbynull.peernetic.demos.chord.core.ChordUtils;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.core.FingerTable;
import com.offbynull.peernetic.demos.chord.core.InternalPointer;
import com.offbynull.peernetic.demos.chord.core.Pointer;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Instant;
import java.util.Random;

public final class FixFinger<A> {

    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_ROUTE_TO_FINGER = "route_to_finger";
    public static final String DONE_STATE = "done";

    private FingerTable<A> fingerTable;
            
    private RouteToFinger<A> routeToFinger;
    private FiniteStateMachine routeToFingerFsm;
    
    private int idx;
    
    private Pointer newFinger;

    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, Object unused, ChordContext<A> context) throws Exception {
        this.fingerTable = context.getChordState().getFingerTable();
        
        int maxIdx = ChordUtils.getBitLength(context.getSelfId());
        this.idx = new Random().nextInt(maxIdx);
        
        Id expectedId = fingerTable.getExpectedId(idx);
        Pointer pointer = fingerTable.findClosestPreceding(expectedId);
        
        if (pointer instanceof InternalPointer) {
            // internal pointer, return self
            fsm.setState(DONE_STATE);
        } else if (pointer instanceof ExternalPointer) {
            ExternalPointer<A> fromNode = (ExternalPointer<A>) pointer;

            routeToFinger = new RouteToFinger<>(fromNode, expectedId);
            routeToFingerFsm = new FiniteStateMachine(routeToFinger, RouteToFinger.INITIAL_STATE, Endpoint.class);
            routeToFingerFsm.process(time, new Object(), context);
            
            fsm.setState(AWAIT_ROUTE_TO_FINGER);
        } else {
            throw new IllegalStateException();
        }
    }

    @FilterHandler({AWAIT_ROUTE_TO_FINGER})
    public boolean filterResponses(FiniteStateMachine fsm, Instant time, Response response,
            ChordContext<A> context) throws Exception {
        return context.getOutgoingRequestManager().isExpectedResponse(time, response);
    }

    @StateHandler(AWAIT_ROUTE_TO_FINGER)
    public void handleRouteToFingerResponse(FiniteStateMachine fsm, Instant time, Object message, ChordContext<A> context)
            throws Exception {
        routeToFingerFsm.process(time, message, context);
        
        if (routeToFingerFsm.getState().equals(RouteToFinger.DONE_STATE)) {
            ExternalPointer<A> foundFinger = routeToFinger.getResult();
            newFinger = foundFinger;
            fsm.setState(DONE_STATE);
        }
    }

    public int getIndex() {
        return idx;
    }

    public Pointer getNewFinger() {
        return newFinger;
    }
}
