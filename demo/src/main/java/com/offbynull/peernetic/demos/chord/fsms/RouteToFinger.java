package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.DurationUtils;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.IncomingRequestManager;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Response;
import com.offbynull.peernetic.demos.chord.core.ChordUtils;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdResponse;
import com.offbynull.peernetic.demos.chord.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToFinger<A> {
    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_PREDECESSOR_RESPONSE_STATE = "pred_await";
    public static final String AWAIT_SUCCESSOR_RESPONSE_STATE = "succ_resp";
    public static final String AWAIT_ID_RESPONSE_STATE = "id_resp";
    public static final String DONE_STATE = "done";

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private final Id findId;
    
    private Id foundId;
    private A foundAddress;
    
    private ExternalPointer<A> currentNode;

    private final IncomingRequestManager<A, byte[]> incomingRequestManager;
    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;

    public RouteToFinger(ExternalPointer<A> initialNode, Id findId, EndpointDirectory<A> endpointDirectory,
            EndpointIdentifier<A> endpointIdentifier, EndpointScheduler endpointScheduler, Endpoint selfEndpoint,
            NonceGenerator<byte[]> nonceGenerator, NonceWrapper<byte[]> nonceWrapper) {
        Validate.notNull(initialNode);
        Validate.notNull(findId);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceWrapper);
        
        this.findId = findId;
        this.currentNode = initialNode;
        this.incomingRequestManager = new IncomingRequestManager<>(selfEndpoint, nonceWrapper);
        this.outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory);
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
    }
    
    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint)
            throws Exception {
        byte[] idData = currentNode.getId().getValueAsByteArray();
        outgoingRequestManager.sendRequestAndTrack(instant, new GetClosestPrecedingFingerRequest(idData), currentNode.getAddress());
        fsm.setState(AWAIT_PREDECESSOR_RESPONSE_STATE);
        
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
    }

    @FilterHandler({AWAIT_PREDECESSOR_RESPONSE_STATE, AWAIT_SUCCESSOR_RESPONSE_STATE, AWAIT_ID_RESPONSE_STATE})
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response, Endpoint srcEndpoint)
            throws Exception {
        return outgoingRequestManager.testResponseMessage(instant, response);
    }

    @StateHandler(AWAIT_PREDECESSOR_RESPONSE_STATE)
    public void handleFindPredecessorResponse(String state, FiniteStateMachine fsm, Instant instant,
            GetClosestPrecedingFingerResponse<A> response, Endpoint srcEndpoint) throws Exception {
        A address = response.getAddress();
        byte[] idData = response.getId();
        
        Id id = new Id(idData, currentNode.getId().getLimitAsByteArray());
        if (id.equals(currentNode.getId()) && address == null) {
            // node that's returned is currentNode...  we can't go anywhere else so return currentNode as the routed node
            foundId = currentNode.getId();
            foundAddress = currentNode.getAddress();
            fsm.setState(DONE_STATE);
        } else if (!id.equals(currentNode.getId()) && address != null) {
            ExternalPointer<A> nextNode = new ExternalPointer<>(id, address);
            currentNode = nextNode;
            if (findId.isWithin(currentNode.getId(), false, nextNode.getId(), true)) {
                // node found, stop here
                outgoingRequestManager.sendRequestAndTrack(instant, new GetSuccessorRequest(), currentNode.getAddress());
                fsm.setState(AWAIT_SUCCESSOR_RESPONSE_STATE);
            } else {
                outgoingRequestManager.sendRequestAndTrack(instant, new GetClosestPrecedingFingerRequest(idData), currentNode.getAddress());
                fsm.setState(AWAIT_PREDECESSOR_RESPONSE_STATE);
            }
        } else {
            // we have a node id that isn't current node and no address, node gave us bad response so stop
            throw new IllegalStateException();
        }
    }

    @StateHandler(AWAIT_SUCCESSOR_RESPONSE_STATE)
    public void handleSuccessorResponse(String state, FiniteStateMachine fsm, Instant instant,
            GetClosestPrecedingFingerResponse<A> response, Endpoint srcEndpoint) throws Exception {
        A address = response.getAddress();
        outgoingRequestManager.sendRequestAndTrack(instant, new GetIdRequest(), address);
        fsm.setState(AWAIT_ID_RESPONSE_STATE);
    }

    @StateHandler(AWAIT_ID_RESPONSE_STATE)
    public void handleAskForIdResponse(String state, FiniteStateMachine fsm, Instant instant, GetIdResponse response,
            Endpoint srcEndpoint) throws Exception {
        int bitSize = ChordUtils.getBitLength(findId);
        foundId = new Id(response.getId(), bitSize);
        foundAddress = endpointIdentifier.identify(srcEndpoint);
        fsm.setState(DONE_STATE);
    }

    @StateHandler({AWAIT_PREDECESSOR_RESPONSE_STATE, AWAIT_SUCCESSOR_RESPONSE_STATE, AWAIT_ID_RESPONSE_STATE})
    public void handleTimerTrigger(String state, FiniteStateMachine fsm, Instant instant, TimerTrigger message, Endpoint srcEndpoint) {
        if (!message.checkParent(this)) {
            return;
        }
        
        Duration irmDuration = incomingRequestManager.process(instant);
        Duration ormDuration = outgoingRequestManager.process(instant);
        
        if (outgoingRequestManager.getPending() == 0) {
            fsm.setState(DONE_STATE);
            return;
        }
        
        Duration nextDuration = DurationUtils.scheduleEarliestDuration(irmDuration, ormDuration, TIMER_DURATION);
        endpointScheduler.scheduleMessage(nextDuration, selfEndpoint, selfEndpoint, new TimerTrigger());
    }

    public ExternalPointer<A> getResult() {
        if (foundId == null || foundAddress == null) {
            return null;
        }
        
        return new ExternalPointer<>(foundId, foundAddress);
    }

    public final class TimerTrigger {
        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }
        
        public boolean checkParent(Object obj) {
            return RouteToFinger.this == obj;
        }
    }
}
