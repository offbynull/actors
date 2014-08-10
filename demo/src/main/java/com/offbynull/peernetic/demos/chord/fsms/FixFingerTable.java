package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Response;
import com.offbynull.peernetic.demos.chord.core.ChordState;
import com.offbynull.peernetic.demos.chord.core.ChordUtils;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.core.InternalPointer;
import com.offbynull.peernetic.demos.chord.core.Pointer;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class FixFingerTable<A> {

    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_ROUTE_TO_FINGER = "route_to_finger";
    public static final String DONE_STATE = "done";

    private final ChordState<A> chordState;

    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointDirectory<A> endpointDirectory;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final NonceGenerator<byte[]> nonceGenerator;
    private final NonceWrapper<byte[]> nonceWrapper;
    
    private RouteToFinger<A> routeToFinger;
    private FiniteStateMachine routeToFingerFsm;

    private final int idx;

    public FixFingerTable(Id selfId, int idx, ChordState<A> chordState, EndpointDirectory<A> endpointDirectory,
            EndpointScheduler endpointScheduler, Endpoint selfEndpoint, NonceGenerator<byte[]> nonceGenerator,
            NonceWrapper<byte[]> nonceWrapper) {
        Validate.notNull(chordState);
        Validate.notNull(selfId);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceWrapper);
        Validate.isTrue(idx >= 0 && idx <= ChordUtils.getBitLength(selfId));

        this.idx = idx;
        this.chordState = chordState;
        this.endpointDirectory = endpointDirectory;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
        this.nonceGenerator = nonceGenerator;
        this.nonceWrapper = nonceWrapper;
        this.outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory);
    }

    @FilterHandler({AWAIT_ROUTE_TO_FINGER})
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response,
            Endpoint srcEndpoint) throws Exception {
        return outgoingRequestManager.testResponseMessage(instant, response);
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) throws Exception {
        Id expectedId = chordState.getExpectedFingerId(idx);
        Pointer pointer = chordState.getClosestPreceding(expectedId);
        
        if (pointer instanceof InternalPointer) {
            // internal pointer, return self
        } else if (pointer instanceof ExternalPointer) {
            ExternalPointer<A> fromNode = (ExternalPointer<A>) pointer;

            routeToFinger = new RouteToFinger<>(fromNode, expectedId, endpointDirectory, endpointScheduler, selfEndpoint, nonceGenerator,
                    nonceWrapper);
            routeToFingerFsm = new FiniteStateMachine(routeToFinger, RouteToFinger.INITIAL_STATE, Endpoint.class);
            routeToFingerFsm.process(instant, new Object(), NullEndpoint.INSTANCE);
        } else {
            throw new IllegalStateException();
        }
        
        fsm.setState(AWAIT_ROUTE_TO_FINGER);
    }

    @StateHandler(AWAIT_ROUTE_TO_FINGER)
    public void handleRouteToFingerResponse(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint)
            throws Exception {
        routeToFingerFsm.process(instant, message, srcEndpoint);
        
        if (routeToFingerFsm.getState().equals(RouteToFinger.DONE_STATE)) {
            ExternalPointer<A> foundFinger = routeToFinger.getResult();
            chordState.putFinger(foundFinger);
            fsm.setState(DONE_STATE);
        }
    }

    public static final class TimerTrigger {

        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }
    }
}
