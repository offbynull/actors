package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Response;
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
import org.apache.commons.lang3.Validate;

public final class FixFinger<A> {

    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_ROUTE_TO_FINGER = "route_to_finger";
    public static final String DONE_STATE = "done";

    private final FingerTable<A> fingerTable;

    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final NonceWrapper<byte[]> nonceWrapper;
            
    private RouteToFinger<A> routeToFinger;
    private FiniteStateMachine routeToFingerFsm;
    
    private final int idx;
    
    private Pointer newFinger;

    public FixFinger(Id selfId, FingerTable<A> fingerTable, EndpointIdentifier<A> endpointIdentifier, EndpointScheduler endpointScheduler,
            Endpoint selfEndpoint, NonceWrapper<byte[]> nonceWrapper, OutgoingRequestManager<A, byte[]> outgoingRequestManager) {
        Validate.notNull(fingerTable);
        Validate.notNull(selfId);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceWrapper);
        Validate.notNull(outgoingRequestManager);

        int maxIdx = ChordUtils.getBitLength(selfId);
        this.idx = new Random().nextInt(maxIdx);
        
        this.fingerTable = fingerTable;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
        this.nonceWrapper = nonceWrapper;
        this.outgoingRequestManager = outgoingRequestManager;
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) throws Exception {
        Id expectedId = fingerTable.getExpectedId(idx);
        Pointer pointer = fingerTable.findClosestPreceding(expectedId);
        
        if (pointer instanceof InternalPointer) {
            // internal pointer, return self
            fsm.setState(DONE_STATE);
        } else if (pointer instanceof ExternalPointer) {
            ExternalPointer<A> fromNode = (ExternalPointer<A>) pointer;

            routeToFinger = new RouteToFinger<>(fromNode, expectedId, endpointIdentifier, endpointScheduler,
                    selfEndpoint, nonceWrapper, outgoingRequestManager);
            routeToFingerFsm = new FiniteStateMachine(routeToFinger, RouteToFinger.INITIAL_STATE, Endpoint.class);
            routeToFingerFsm.process(instant, new Object(), NullEndpoint.INSTANCE);
            
            fsm.setState(AWAIT_ROUTE_TO_FINGER);
        } else {
            throw new IllegalStateException();
        }
    }

    @FilterHandler({AWAIT_ROUTE_TO_FINGER})
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response,
            Endpoint srcEndpoint) throws Exception {
        return outgoingRequestManager.isMessageTracked(instant, response);
    }

    @StateHandler(AWAIT_ROUTE_TO_FINGER)
    public void handleRouteToFingerResponse(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint)
            throws Exception {
        routeToFingerFsm.process(instant, message, srcEndpoint);
        
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
