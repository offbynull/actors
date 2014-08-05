package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.IncomingRequestManager;
import com.offbynull.peernetic.common.Message;
import com.offbynull.peernetic.common.NonceGenerator;
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
    public static final String SEND_QUERY_FOR_ID = "ask_for_id";
    public static final String AWAIT_QUERY_FOR_ID = "wait_for_id_response";
    public static final String START_ROUTE_TO_FINGER = "route_to_finger";
    public static final String AWAIT_ROUTE_TO_FINGER = "route_to_finger";
    public static final String DONE_STATE = "done";

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private final FingerTable<A> fingerTable;

    private final IncomingRequestManager<A, byte[]> incomingRequestManager;
    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointDirectory<A> endpointDirectory;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final NonceGenerator<byte[]> nonceGenerator;
    private final NonceWrapper<byte[]> nonceWrapper;

    private A initialAddress;
    private Id initialId;
    
    private RouteToFinger<A> routeToFinger;
    private FiniteStateMachine routeToFingerFsm;

    private final int maxIdx;
    private int idx;

    ALL OF THIS CLASS IS UNTESTED;
    public InitFingerTable(Id selfId, A initialAddress, EndpointDirectory<A> endpointDirectory, EndpointScheduler endpointScheduler,
            Endpoint selfEndpoint, NonceGenerator<byte[]> nonceGenerator, NonceWrapper<byte[]> nonceWrapper) {
        Validate.notNull(initialAddress);
        Validate.notNull(selfId);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceWrapper);

        this.initialAddress = initialAddress;
        this.endpointDirectory = endpointDirectory;
        this.endpointScheduler = endpointScheduler;
        this.fingerTable = new FingerTable<>(new InternalPointer(selfId));
        this.selfEndpoint = selfEndpoint;
        this.nonceGenerator = nonceGenerator;
        this.nonceWrapper = nonceWrapper;
        this.incomingRequestManager = new IncomingRequestManager<>(selfEndpoint, nonceWrapper);
        this.outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory);

        maxIdx = ChordUtils.getBitLength(selfId);
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) {
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
        fsm.switchStateAndProcess(SEND_QUERY_FOR_ID, instant, unused, srcEndpoint);
    }

    @FilterHandler({SEND_QUERY_FOR_ID, AWAIT_QUERY_FOR_ID})
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response,
            Endpoint srcEndpoint) throws Exception {
        return outgoingRequestManager.testResponseMessage(instant, response);
    }
    
    @StateHandler(SEND_QUERY_FOR_ID)
    public void handleSendAskForIdRequest(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint)
            throws Exception {
        outgoingRequestManager.sendRequestAndTrack(instant, new GetIdRequest(), initialAddress);
        fsm.setState(AWAIT_QUERY_FOR_ID);
    }

    @StateHandler(AWAIT_QUERY_FOR_ID)
    public void handleReceiveAskForIdResponse(String state, FiniteStateMachine fsm, Instant instant, GetIdResponse response,
            Endpoint srcEndpoint) throws Exception {
        initialId = new Id(response.getId(), maxIdx);
        fsm.switchStateAndProcess(START_ROUTE_TO_FINGER, instant, new Object()/*unused obj*/, srcEndpoint);
    }

    @StateHandler(START_ROUTE_TO_FINGER)
    public void handleStartRoute(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint)
            throws Exception {
        ExternalPointer<A> fromNode = new ExternalPointer<>(initialId, initialAddress);
        Id findId = fingerTable.getExpectedId(idx);
        routeToFinger = new RouteToFinger<>(fromNode, findId, endpointDirectory, endpointScheduler, selfEndpoint, nonceGenerator,
                nonceWrapper);
        fsm.setState(AWAIT_ROUTE_TO_FINGER);
    }

    @StateHandler(AWAIT_ROUTE_TO_FINGER)
    public void handleAskForIdRequest(String state, FiniteStateMachine fsm, Instant instant, Message message, Endpoint srcEndpoint)
            throws Exception {
        routeToFingerFsm.process(instant, message, srcEndpoint);
        
        if (routeToFinger.isFinishedRunning()) {
            ExternalPointer<A> foundFinger = routeToFinger.getResult();
            fingerTable.replace(foundFinger);

            idx++;
            if (idx == maxIdx) {
                fsm.setState(DONE_STATE);
            } else {
                fsm.switchStateAndProcess(START_ROUTE_TO_FINGER, instant, message, srcEndpoint);
            }
        }
    }

    @StateHandler({SEND_QUERY_FOR_ID, AWAIT_QUERY_FOR_ID})
    public void handleTimerTrigger(String state, FiniteStateMachine fsm, Instant instant, TimerTrigger response, Endpoint srcEndpoint) {
        incomingRequestManager.process(instant);
        outgoingRequestManager.process(instant);

        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
    }

    public static final class TimerTrigger {

        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }
    }
}
