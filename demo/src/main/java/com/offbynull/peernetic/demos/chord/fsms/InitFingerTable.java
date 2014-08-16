package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.DurationUtils;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Response;
import com.offbynull.peernetic.demos.chord.core.ChordUtils;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.core.FingerTable;
import com.offbynull.peernetic.demos.chord.core.InternalPointer;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdResponse;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class InitFingerTable<A> {

    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_GET_ID_RESPONSE = "wait_for_id_response";
    public static final String AWAIT_ROUTE_TO_FINGER = "route_to_finger";
    public static final String DONE_STATE = "done";

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private final FingerTable<A> fingerTable;

    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final NonceWrapper<byte[]> nonceWrapper;

    private A initialAddress;
    private Id initialId;
    
    private RouteToFinger<A> routeToFinger;
    private FiniteStateMachine routeToFingerFsm;

    private final int maxIdx;
    private int idx;

    public InitFingerTable(Id selfId, A initialAddress, EndpointIdentifier<A> endpointIdentifier, EndpointScheduler endpointScheduler,
            Endpoint selfEndpoint, NonceWrapper<byte[]> nonceWrapper, OutgoingRequestManager<A, byte[]> outgoingRequestManager) {
        Validate.notNull(initialAddress);
        Validate.notNull(selfId);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceWrapper);
        Validate.notNull(outgoingRequestManager);

        this.initialAddress = initialAddress;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.fingerTable = new FingerTable<>(new InternalPointer(selfId));
        this.selfEndpoint = selfEndpoint;
        this.nonceWrapper = nonceWrapper;
        this.outgoingRequestManager = outgoingRequestManager;

        maxIdx = ChordUtils.getBitLength(selfId);
    }

    @FilterHandler(AWAIT_GET_ID_RESPONSE)
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response,
            Endpoint srcEndpoint) throws Exception {
        return outgoingRequestManager.isMessageTracked(instant, response);
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) throws Exception {
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
        outgoingRequestManager.sendRequestAndTrack(instant, new GetIdRequest(), initialAddress);
        fsm.setState(AWAIT_GET_ID_RESPONSE);
    }

    @StateHandler(AWAIT_GET_ID_RESPONSE)
    public void handleGetIdResponse(String state, FiniteStateMachine fsm, Instant instant, GetIdResponse response,
            Endpoint srcEndpoint) throws Exception {
        initialId = new Id(response.getId(), maxIdx);
        
        resetRouteToFinger(instant);
        fsm.setState(AWAIT_ROUTE_TO_FINGER);
    }

    @StateHandler(AWAIT_ROUTE_TO_FINGER)
    public void handleRouteToFingerResponse(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint)
            throws Exception {
        routeToFingerFsm.process(instant, message, srcEndpoint);
        
        if (routeToFingerFsm.getState().equals(RouteToFinger.DONE_STATE)) {
            ExternalPointer<A> foundFinger = routeToFinger.getResult();
            fingerTable.replace(foundFinger);

            idx++;
            if (idx == maxIdx) {
                fsm.setState(DONE_STATE);
            } else {
                resetRouteToFinger(instant);
                fsm.setState(AWAIT_ROUTE_TO_FINGER);
            }
        }
    }
    
    private void resetRouteToFinger(Instant instant) {
        ExternalPointer<A> fromNode = new ExternalPointer<>(initialId, initialAddress);
        Id findId = fingerTable.getExpectedId(idx);
        routeToFinger = new RouteToFinger<>(fromNode, findId, endpointIdentifier, endpointScheduler, selfEndpoint, nonceWrapper,
                outgoingRequestManager);
        routeToFingerFsm = new FiniteStateMachine(routeToFinger, RouteToFinger.INITIAL_STATE, Endpoint.class);
        routeToFingerFsm.process(instant, new Object(), NullEndpoint.INSTANCE);
    }

    @StateHandler(AWAIT_GET_ID_RESPONSE)
    public void handleTimerTrigger(String state, FiniteStateMachine fsm, Instant instant, TimerTrigger message, Endpoint srcEndpoint) {
        if (!message.checkParent(this)) {
            return;
        }

        Duration ormDuration = outgoingRequestManager.process(instant);
        
        Duration nextDuration = DurationUtils.scheduleEarliestDuration(ormDuration, TIMER_DURATION);
        endpointScheduler.scheduleMessage(nextDuration, selfEndpoint, selfEndpoint, new TimerTrigger());
    }

    public FingerTable<A> getFingerTable() {
        return fingerTable;
    }
    
    public final class TimerTrigger {
        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }
        
        public boolean checkParent(Object obj) {
            return InitFingerTable.this == obj;
        }
    }
}
