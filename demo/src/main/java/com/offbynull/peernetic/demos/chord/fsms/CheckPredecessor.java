package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.common.message.ByteArrayNonce;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceManager;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.demos.chord.ChordContext;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdResponse;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;

public final class CheckPredecessor<A> {

    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_GET_ID = "get_id";
    public static final String DONE_STATE = "done";

    private ExternalPointer<A> existingPredecessor;
    private Id newPredecessorId;

    private NonceManager<byte[]> nonceManager = new NonceManager<>();
    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, Object unused, ChordContext<A> context) throws Exception {
        existingPredecessor = (ExternalPointer<A>) context.getChordState().getPredecessor();
        
        if (existingPredecessor == null) {
            fsm.setState(DONE_STATE);
            return;
        }
        
        Nonce<byte[]> nonce = context.getOutgoingRequestManager().sendRequestAndTrack(
                time, new GetIdRequest(), existingPredecessor.getAddress());
        nonceManager.addNonce(time, Duration.ofSeconds(30L), nonce, null);
        Duration duration = context.getOutgoingRequestManager().process(time);
        context.getEndpointScheduler().scheduleMessage(duration, context.getSelfEndpoint(), context.getSelfEndpoint(), new TimerTrigger());
        fsm.setState(AWAIT_GET_ID);
    }

    @FilterHandler(AWAIT_GET_ID)
    public boolean filterResponses(FiniteStateMachine fsm, Instant time, Response response, ChordContext<A> context) throws Exception {
        return context.getOutgoingRequestManager().isExpectedResponse(time, response);
    }

    @StateHandler(AWAIT_GET_ID)
    public void handleGetIdResponse(FiniteStateMachine fsm, Instant time, GetIdResponse response, ChordContext<A> context)
            throws Exception {
        ByteArrayNonce nonce = new ByteArrayNonce(response.getNonce());
        if (!nonceManager.isNoncePresent(nonce)) {
            return;
        }

        newPredecessorId = new Id(response.getId(), context.getSelfId().getLimitAsByteArray());
        fsm.setState(DONE_STATE);
    }

    @StateHandler(AWAIT_GET_ID)
    public void handleTimer(FiniteStateMachine fsm, Instant time, TimerTrigger message, ChordContext<A> context) throws Exception {
        if (!message.checkParent(this)) {
            return;
        }
        
        Duration duration = context.getOutgoingRequestManager().process(time);
        if (context.getOutgoingRequestManager().getPending() == 0) {
            fsm.setState(DONE_STATE);
            return;
        }
        context.getEndpointScheduler().scheduleMessage(duration, context.getSelfEndpoint(), context.getSelfEndpoint(), message);
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
