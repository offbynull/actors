package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.message.ByteArrayNonce;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceManager;
import com.offbynull.peernetic.common.transmission.OutgoingRequestManager;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdResponse;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class CheckPredecessor<A> {

    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_GET_ID = "get_id";
    public static final String DONE_STATE = "done";

    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final Id selfId;
    
    private final ExternalPointer<A> existingPredecessor;
    private Id newPredecessorId;

    public CheckPredecessor(Id selfId, ExternalPointer<A> predecessor,  EndpointScheduler endpointScheduler, Endpoint selfEndpoint,
            OutgoingRequestManager<A, byte[]> outgoingRequestManager) {
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(selfId);
        Validate.notNull(outgoingRequestManager);
        
        this.existingPredecessor = predecessor;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
        this.selfId = selfId;
        this.outgoingRequestManager = outgoingRequestManager;
    }

    private NonceManager<byte[]> nonceManager = new NonceManager<>();
    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) throws Exception {
        if (existingPredecessor == null) {
            fsm.setState(DONE_STATE);
            return;
        }
        
        Nonce<byte[]> nonce = outgoingRequestManager.sendRequestAndTrack(instant, new GetIdRequest(), existingPredecessor.getAddress());
        nonceManager.addNonce(instant, Duration.ofSeconds(30L), nonce, null);
        Duration duration = outgoingRequestManager.process(instant);
        endpointScheduler.scheduleMessage(duration, selfEndpoint, selfEndpoint, new TimerTrigger());
        fsm.setState(AWAIT_GET_ID);
    }

    @FilterHandler(AWAIT_GET_ID)
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response,
            Endpoint srcEndpoint) throws Exception {
        return outgoingRequestManager.isMessageTracked(instant, response);
    }

    @StateHandler(AWAIT_GET_ID)
    public void handleGetIdResponse(String state, FiniteStateMachine fsm, Instant instant, GetIdResponse response, Endpoint srcEndpoint)
            throws Exception {
        ByteArrayNonce nonce = new ByteArrayNonce(response.getNonce());
        if (nonceManager.checkNonce(nonce) == null) {
            return;
        }

        newPredecessorId = new Id(response.getId(), selfId.getLimitAsByteArray());
        fsm.setState(DONE_STATE);
    }

    @StateHandler(AWAIT_GET_ID)
    public void handleTimer(String state, FiniteStateMachine fsm, Instant instant, TimerTrigger message, Endpoint srcEndpoint)
            throws Exception {
        if (!message.checkParent(this)) {
            return;
        }
        
        Duration duration = outgoingRequestManager.process(instant);
        if (outgoingRequestManager.getPending() == 0) {
            fsm.setState(DONE_STATE);
            return;
        }
        endpointScheduler.scheduleMessage(duration, srcEndpoint, srcEndpoint, message);
    }

    public ExternalPointer<A> getExistingPredecessor() {
        return existingPredecessor;
    }
    
    public boolean isPredecessorUnresponsive() {
        return existingPredecessor == null || newPredecessorId == null || !newPredecessorId.equals(existingPredecessor.getId());
    }

    public final class TimerTrigger {
        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }
        
        public boolean checkParent(Object obj) {
            return CheckPredecessor.this == obj;
        }
    }
}
