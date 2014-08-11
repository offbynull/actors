package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Response;
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
    
    private final ExternalPointer<A> predecessor;
    private Id reportedPredecessorId;

    public CheckPredecessor(Id selfId, ExternalPointer<A> predecessor, EndpointDirectory<A> endpointDirectory,
            EndpointScheduler endpointScheduler, Endpoint selfEndpoint, NonceGenerator<byte[]> nonceGenerator,
            NonceWrapper<byte[]> nonceWrapper) {
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(selfId);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceWrapper);
        
        this.predecessor = predecessor;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
        this.selfId = selfId;
        this.outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory);
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) throws Exception {
        if (predecessor == null) {
            fsm.setState(DONE_STATE);
            return;
        }
        
        outgoingRequestManager.sendRequestAndTrack(instant, new GetIdRequest(), predecessor.getAddress());
        Duration duration = outgoingRequestManager.process(instant);
        endpointScheduler.scheduleMessage(duration, selfEndpoint, selfEndpoint, new TimerTrigger());
        fsm.setState(AWAIT_GET_ID);
    }

    @FilterHandler(AWAIT_GET_ID)
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response,
            Endpoint srcEndpoint) throws Exception {
        return outgoingRequestManager.testResponseMessage(instant, response);
    }

    @StateHandler(AWAIT_GET_ID)
    public void handleGetIdResponse(String state, FiniteStateMachine fsm, Instant instant, GetIdResponse response, Endpoint srcEndpoint)
            throws Exception {
        reportedPredecessorId = new Id(response.getId(), selfId.getLimitAsByteArray());
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

    public boolean isPredecessorUnresponsive() {
        return predecessor == null || reportedPredecessorId == null || !reportedPredecessorId.equals(predecessor.getId());
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
