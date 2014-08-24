package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.common.ProcessableUtils;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.demos.chord.ChordContext;
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

public final class InitFingerTable<A> {

    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_GET_ID_RESPONSE = "wait_for_id_response";
    public static final String AWAIT_ROUTE_TO_FINGER = "route_to_finger";
    public static final String DONE_STATE = "done";

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private FingerTable<A> fingerTable;

    private A initialAddress;
    private Id initialId;
    
    private RouteToFinger<A> routeToFinger;
    private FiniteStateMachine routeToFingerFsm;

    private int maxIdx;
    private int idx;

    @FilterHandler(AWAIT_GET_ID_RESPONSE)
    public boolean filterResponses(FiniteStateMachine fsm, Instant time, Response response, ChordContext<A> context) throws Exception {
        return context.getOutgoingRequestManager().isExpectedResponse(time, response);
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, Object unused, ChordContext<A> context) throws Exception {
        this.initialAddress = context.getBootstrapAddress();
        this.fingerTable = new FingerTable<>(new InternalPointer(context.getSelfId()));

        maxIdx = ChordUtils.getBitLength(context.getSelfId());
        
        context.getEndpointScheduler().scheduleMessage(TIMER_DURATION, context.getSelfEndpoint(), context.getSelfEndpoint(),
                new TimerTrigger());
        context.getOutgoingRequestManager().sendRequestAndTrack(time, new GetIdRequest(), initialAddress);
        fsm.setState(AWAIT_GET_ID_RESPONSE);
    }

    @StateHandler(AWAIT_GET_ID_RESPONSE)
    public void handleGetIdResponse(FiniteStateMachine fsm, Instant time, GetIdResponse response,
            ChordContext<A> context) throws Exception {
        initialId = new Id(response.getId(), maxIdx);
        
        resetRouteToFinger(time, context);
        fsm.setState(AWAIT_ROUTE_TO_FINGER);
    }

    @StateHandler(AWAIT_ROUTE_TO_FINGER)
    public void handleRouteToFingerResponse(FiniteStateMachine fsm, Instant time, Object message, ChordContext<A> context)
            throws Exception {
        routeToFingerFsm.process(time, message, context);
        
        if (routeToFingerFsm.getState().equals(RouteToFinger.DONE_STATE)) {
            ExternalPointer<A> foundFinger = routeToFinger.getResult();
            fingerTable.replace(foundFinger);

            idx++;
            if (idx == maxIdx) {
                fsm.setState(DONE_STATE);
            } else {
                resetRouteToFinger(time, context);
                fsm.setState(AWAIT_ROUTE_TO_FINGER);
            }
        }
    }
    
    private void resetRouteToFinger(Instant time, ChordContext<A> context) {
        ExternalPointer<A> fromNode = new ExternalPointer<>(initialId, initialAddress);
        Id findId = fingerTable.getExpectedId(idx);
        routeToFinger = new RouteToFinger<>(fromNode, findId);
        routeToFingerFsm = new FiniteStateMachine(routeToFinger, RouteToFinger.INITIAL_STATE, ChordContext.class);
        routeToFingerFsm.process(time, new Object(), context);
    }

    @StateHandler(AWAIT_GET_ID_RESPONSE)
    public void handleTimerTrigger(FiniteStateMachine fsm, Instant time, TimerTrigger message, ChordContext<A> context) {
        if (!message.checkParent(this)) {
            return;
        }

        Duration ormDuration = context.getOutgoingRequestManager().process(time);
        
        Duration nextDuration = ProcessableUtils.scheduleEarliestDuration(ormDuration, TIMER_DURATION);
        context.getEndpointScheduler().scheduleMessage(nextDuration, context.getSelfEndpoint(), context.getSelfEndpoint(),
                new TimerTrigger());
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
