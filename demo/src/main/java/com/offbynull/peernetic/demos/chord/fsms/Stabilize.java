package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.demos.chord.ChordContext;
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

public final class Stabilize<A> {
    public static final String INITIAL_STATE = "start";
    public static final String AWAIT_PREDECESSOR_RESPONSE_STATE = "pred_await";
    public static final String DONE_STATE = "done";

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private Pointer existingSuccessor;    
    private Pointer newSuccessor;

    
    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, Object unused, ChordContext<A> context) throws Exception {
        existingSuccessor = context.getChordState().getSuccessor();
        
        if (existingSuccessor.getId().equals(context.getSelfId())) {
            newSuccessor = existingSuccessor;
            fsm.setState(DONE_STATE);
            return;
        }
        
        A successorAddress = ((ExternalPointer<A>) existingSuccessor).getAddress();
        context.getOutgoingRequestManager().sendRequestAndTrack(time, new GetPredecessorRequest(), successorAddress);
        fsm.setState(AWAIT_PREDECESSOR_RESPONSE_STATE);
        
        context.getEndpointScheduler().scheduleMessage(TIMER_DURATION, context.getSelfEndpoint(), context.getSelfEndpoint(),
                new TimerTrigger());
    }

    @FilterHandler({AWAIT_PREDECESSOR_RESPONSE_STATE})
    public boolean filterResponses(FiniteStateMachine fsm, Instant time, Response response, ChordContext<A> context)
            throws Exception {
        return context.getOutgoingRequestManager().isExpectedResponse(time, response);
    }

    @StateHandler(AWAIT_PREDECESSOR_RESPONSE_STATE)
    public void handleGetPredecessorResponse(FiniteStateMachine fsm, Instant time,
            GetPredecessorResponse<A> response, ChordContext<A> context) throws Exception {
        if (response.getId() != null) {
            A address = response.getAddress();
            byte[] idData = response.getId();

            Id potentiallyNewSuccessorId = new Id(idData, context.getSelfId().getLimitAsByteArray());
            Id existingSuccessorId = ((ExternalPointer<A>) existingSuccessor).getId();

            if (potentiallyNewSuccessorId.isWithin(context.getSelfId(), false, existingSuccessorId, false)) {
                // set as new successor and fire-and-forget a notify msg to it
                newSuccessor = new ExternalPointer<>(potentiallyNewSuccessorId, address);
            } else {
                newSuccessor = existingSuccessor;
            }
        } else {
            newSuccessor = existingSuccessor;
        }
        
        if (newSuccessor != null && newSuccessor instanceof ExternalPointer) {
            context.getOutgoingRequestManager().sendRequestAndTrack(time, new NotifyRequest(context.getSelfId().getValueAsByteArray()),
                    ((ExternalPointer<A>)newSuccessor).getAddress());
        }
        
        fsm.setState(DONE_STATE);
    }

    @StateHandler(AWAIT_PREDECESSOR_RESPONSE_STATE)
    public void handleTimer(FiniteStateMachine fsm, Instant time, TimerTrigger message, ChordContext<A> context)
            throws Exception {
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
