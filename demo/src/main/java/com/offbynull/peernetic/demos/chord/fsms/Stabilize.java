package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.transmission.OutgoingRequestManager;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.core.Pointer;
import com.offbynull.peernetic.demos.chord.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.demos.chord.messages.external.NotifyRequest;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class Stabilize<A> {
    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_PREDECESSOR_RESPONSE_STATE = "pred_await";
    public static final String DONE_STATE = "done";

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private final Id selfId;
    private final Pointer existingSuccessor;

    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    
    private Pointer newSuccessor;

    public Stabilize(Id selfId, Pointer successor, EndpointScheduler endpointScheduler, Endpoint selfEndpoint,
            OutgoingRequestManager<A, byte[]> outgoingRequestManager) {
        Validate.notNull(selfId);
        Validate.notNull(successor);
        Validate.notNull(outgoingRequestManager);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        
        this.selfId = selfId;
        this.existingSuccessor = successor;
        this.outgoingRequestManager = outgoingRequestManager;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
    }
    
    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, Object unused, Endpoint srcEndpoint) throws Exception {
        if (existingSuccessor.getId().equals(selfId)) {
            newSuccessor = existingSuccessor;
            fsm.setState(DONE_STATE);
            return;
        }
        
        A successorAddress = ((ExternalPointer<A>) existingSuccessor).getAddress();
        outgoingRequestManager.sendRequestAndTrack(time, new GetPredecessorRequest(), successorAddress);
        fsm.setState(AWAIT_PREDECESSOR_RESPONSE_STATE);
        
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
    }

    @FilterHandler({AWAIT_PREDECESSOR_RESPONSE_STATE})
    public boolean filterResponses(FiniteStateMachine fsm, Instant time, Response response, Endpoint srcEndpoint)
            throws Exception {
        return outgoingRequestManager.isMessageTracked(time, response);
    }

    @StateHandler(AWAIT_PREDECESSOR_RESPONSE_STATE)
    public void handleGetPredecessorResponse(FiniteStateMachine fsm, Instant time,
            GetPredecessorResponse<A> response, Endpoint srcEndpoint) throws Exception {
        if (response.getId() != null) {
            A address = response.getAddress();
            byte[] idData = response.getId();

            Id potentiallyNewSuccessorId = new Id(idData, selfId.getLimitAsByteArray());
            Id existingSuccessorId = ((ExternalPointer<A>) existingSuccessor).getId();

            if (potentiallyNewSuccessorId.isWithin(selfId, false, existingSuccessorId, false)) {
                // set as new successor and fire-and-forget a notify msg to it
                newSuccessor = new ExternalPointer<>(potentiallyNewSuccessorId, address);
            } else {
                newSuccessor = existingSuccessor;
            }
        } else {
            newSuccessor = existingSuccessor;
        }
        
        if (newSuccessor != null && newSuccessor instanceof ExternalPointer) {
            outgoingRequestManager.sendRequestAndTrack(time, new NotifyRequest(selfId.getValueAsByteArray()),
                    ((ExternalPointer<A>)newSuccessor).getAddress());
        }
        
        fsm.setState(DONE_STATE);
    }

    @StateHandler(AWAIT_PREDECESSOR_RESPONSE_STATE)
    public void handleTimer(FiniteStateMachine fsm, Instant time, TimerTrigger message, Endpoint srcEndpoint)
            throws Exception {
        if (!message.checkParent(this)) {
            return;
        }
        
        Duration duration = outgoingRequestManager.process(time);
        if (outgoingRequestManager.getPending() == 0) {
            fsm.setState(DONE_STATE);
            return;
        }
        endpointScheduler.scheduleMessage(duration, srcEndpoint, srcEndpoint, message);
    }
    
    public Pointer getNewSuccessor() {
        return newSuccessor;
    }

    public final class TimerTrigger {
        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }
        
        public boolean checkParent(Object obj) {
            return Stabilize.this == obj;
        }
    }
}
