package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.ProcessableUtils;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.transmission.OutgoingRequestManager;
import com.offbynull.peernetic.common.message.Response;
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

    private final Id selfId;
    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;

    private A initialAddress;
    private Id initialId;
    
    private RouteToFinger<A> routeToFinger;
    private FiniteStateMachine routeToFingerFsm;

    private final int maxIdx;
    private int idx;

    public InitFingerTable(Id selfId, A initialAddress, EndpointIdentifier<A> endpointIdentifier, EndpointScheduler endpointScheduler,
            Endpoint selfEndpoint, OutgoingRequestManager<A, byte[]> outgoingRequestManager) {
        Validate.notNull(initialAddress);
        Validate.notNull(selfId);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(outgoingRequestManager);

        this.initialAddress = initialAddress;
        this.selfId = selfId;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.fingerTable = new FingerTable<>(new InternalPointer(selfId));
        this.selfEndpoint = selfEndpoint;
        this.outgoingRequestManager = outgoingRequestManager;

        maxIdx = ChordUtils.getBitLength(selfId);
    }

    @FilterHandler(AWAIT_GET_ID_RESPONSE)
    public boolean filterResponses(FiniteStateMachine fsm, Instant time, Response response,
            Endpoint srcEndpoint) throws Exception {
        return outgoingRequestManager.isMessageTracked(time, response);
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, Object unused, Endpoint srcEndpoint) throws Exception {
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
        outgoingRequestManager.sendRequestAndTrack(time, new GetIdRequest(), initialAddress);
        fsm.setState(AWAIT_GET_ID_RESPONSE);
    }

    @StateHandler(AWAIT_GET_ID_RESPONSE)
    public void handleGetIdResponse(FiniteStateMachine fsm, Instant time, GetIdResponse response,
            Endpoint srcEndpoint) throws Exception {
        initialId = new Id(response.getId(), maxIdx);
        
        resetRouteToFinger(time);
        fsm.setState(AWAIT_ROUTE_TO_FINGER);
    }

    @StateHandler(AWAIT_ROUTE_TO_FINGER)
    public void handleRouteToFingerResponse(FiniteStateMachine fsm, Instant time, Object message, Endpoint srcEndpoint)
            throws Exception {
        routeToFingerFsm.process(time, message, srcEndpoint);
        
        if (routeToFingerFsm.getState().equals(RouteToFinger.DONE_STATE)) {
            ExternalPointer<A> foundFinger = routeToFinger.getResult();
            fingerTable.replace(foundFinger);

            idx++;
            if (idx == maxIdx) {
                fsm.setState(DONE_STATE);
            } else {
                resetRouteToFinger(time);
                fsm.setState(AWAIT_ROUTE_TO_FINGER);
            }
        }
    }
    
    private void resetRouteToFinger(Instant time) {
        ExternalPointer<A> fromNode = new ExternalPointer<>(initialId, initialAddress);
        Id findId = fingerTable.getExpectedId(idx);
        routeToFinger = new RouteToFinger<>(fromNode, selfId, findId, endpointIdentifier, endpointScheduler, selfEndpoint,
                outgoingRequestManager);
        routeToFingerFsm = new FiniteStateMachine(routeToFinger, RouteToFinger.INITIAL_STATE, Endpoint.class);
        routeToFingerFsm.process(time, new Object(), NullEndpoint.INSTANCE);
    }

    @StateHandler(AWAIT_GET_ID_RESPONSE)
    public void handleTimerTrigger(FiniteStateMachine fsm, Instant time, TimerTrigger message, Endpoint srcEndpoint) {
        if (!message.checkParent(this)) {
            return;
        }

        Duration ormDuration = outgoingRequestManager.process(time);
        
        Duration nextDuration = ProcessableUtils.scheduleEarliestDuration(ormDuration, TIMER_DURATION);
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
