package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyRequest;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.coroutines.SleepCoroutine;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StabilizeTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(StabilizeTask.class);
    
    private final String sourceId;
    private final State state;

    public StabilizeTask(String sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        NodeId selfId = state.getSelfId();
        
        Context ctx = (Context) cnt.getContext();
        
        while (true) {
            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
            
            try {
                Pointer successor = state.getSuccessor();
                if (state.isSelfId(successor.getId())) {
                    continue;
                }

                // ask for successor's pred
                String successorAddress = ((ExternalPointer) successor).getAddress();
                
                LOG.debug("{} {} - Requesting successor's ({}) predecessor", state.getSelfId(), sourceId, successor);
                
                GetPredecessorResponse gpr = funnelToRequestCoroutine(
                        cnt,
                        successorAddress,
                        new GetPredecessorRequest(state.generateExternalMessageId()),
                        Duration.ofSeconds(10L),
                        GetPredecessorResponse.class);
                
                LOG.debug("{} {} - Successor's ({}) predecessor is {}", state.getSelfId(), sourceId, successor, gpr.getChordId());

                // check to see if predecessor is between us and our successor
                if (gpr.getChordId() != null) {
                    String address = gpr.getAddress();
                    NodeId potentiallyNewSuccessorId = gpr.getChordId();
                    NodeId existingSuccessorId = ((ExternalPointer) successor).getId();

                    if (potentiallyNewSuccessorId.isWithin(selfId, false, existingSuccessorId, false)) {
                        // it is between us and our successor, so update
                        ExternalPointer newSuccessor = new ExternalPointer(potentiallyNewSuccessorId, address);
                        state.setSuccessor(newSuccessor);

                        successor = newSuccessor;
                        successorAddress = newSuccessor.getAddress();
                    }
                }

                // successor may have been updated by block above
                // ask successor for its successors
                LOG.debug("{} {} - Requesting successor's ({}) successor", state.getSelfId(), sourceId, successor);
                GetSuccessorResponse gsr = funnelToRequestCoroutine(
                        cnt,
                        successorAddress,
                        new GetSuccessorRequest(state.generateExternalMessageId()),
                        Duration.ofSeconds(10L),
                        GetSuccessorResponse.class);

                List<Pointer> subsequentSuccessors = new ArrayList<>();
                gsr.getEntries().stream().map(x -> {
                    NodeId id = x.getChordId();

                    if (x instanceof InternalSuccessorEntry) {
                        return new InternalPointer(id);
                    } else if (x instanceof ExternalSuccessorEntry) {
                        return new ExternalPointer(id, ((ExternalSuccessorEntry) x).getAddress());
                    } else {
                        throw new IllegalStateException();
                    }
                }).forEachOrdered(x -> subsequentSuccessors.add(x));
                
                LOG.debug("{} {} - Successor's ({}) successor is {}", state.getSelfId(), sourceId, successor, subsequentSuccessors);

                // mark it as our new successor
                state.updateSuccessor((ExternalPointer) successor, subsequentSuccessors);
                LOG.debug("{} {} - Successors after stabilization are {}", state.getSelfId(), sourceId, state.getSuccessors());
                
                
                // notify it that we're its predecessor
                addOutgoingExternalMessage(
                        ctx,
                        successorAddress,
                        new NotifyRequest(state.generateExternalMessageId(), selfId));
                LOG.debug("{} {} - Notified {} that we're its successor", state.getSelfId(), sourceId, state.getSuccessor());
            } catch (RuntimeException re) {
                LOG.warn("Failed to stabilize", re);
            }
        }
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        
        SleepCoroutine sleepCoroutine = new SleepCoroutine(sourceId, state.getTimerPrefix(), duration);
        sleepCoroutine.run(cnt);
    }

    private <T extends ExternalMessage> T funnelToRequestCoroutine(Continuation cnt, String destination, ExternalMessage message,
            Duration timeoutDuration, Class<T> expectedResponseClass) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.notNull(timeoutDuration);
        Validate.isTrue(!timeoutDuration.isNegative());
        
        RequestCoroutine requestCoroutine = new RequestCoroutine(
                AddressUtils.parentize(sourceId, "" + message.getId()),
                destination,
                message,
                state.getTimerPrefix(),
                timeoutDuration,
                expectedResponseClass);
        requestCoroutine.run(cnt);
        return requestCoroutine.getResponse();
    }
    
    private void addOutgoingExternalMessage(Context ctx, String destination, ExternalMessage message) {
        Validate.notNull(ctx);
        Validate.notNull(destination);
        Validate.notNull(message);
        
        ctx.addOutgoingMessage(
                AddressUtils.parentize(sourceId, "" + message.getId()),
                destination,
                message);
    }
}
