package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class StabilizeTask implements Subcoroutine<Void> {
    
    private final Address sourceId;
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;

    public StabilizeTask(Address sourceId, State state, Address timerAddress, Address logAddress) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        this.sourceId = sourceId;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
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
                Address successorAddress = ((ExternalPointer) successor).getAddress();
                
                ctx.addOutgoingMessage(sourceId, logAddress,
                        debug("{} {} - Requesting successor's ({}) predecessor", state.getSelfId(), sourceId, successor));
                
                GetPredecessorResponse gpr = funnelToRequestCoroutine(
                        cnt,
                        successorAddress,
                        new GetPredecessorRequest(),
                        GetPredecessorResponse.class);
                
                ctx.addOutgoingMessage(sourceId, logAddress,
                        debug("{} {} - Successor's ({}) predecessor is {}", state.getSelfId(), sourceId, successor, gpr.getChordId()));

                // check to see if predecessor is between us and our successor
                if (gpr.getChordId() != null) {
                    Address address = gpr.getAddress();
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
                ctx.addOutgoingMessage(sourceId, logAddress,
                        debug("{} {} - Requesting successor's ({}) successor", state.getSelfId(), sourceId, successor));
                GetSuccessorResponse gsr = funnelToRequestCoroutine(
                        cnt,
                        successorAddress,
                        new GetSuccessorRequest(),
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
                
                ctx.addOutgoingMessage(sourceId, logAddress,
                        debug("{} {} - Successor's ({}) successor is {}", state.getSelfId(), sourceId, successor, subsequentSuccessors));

                // mark it as our new successor
                state.updateSuccessor((ExternalPointer) successor, subsequentSuccessors);
                ctx.addOutgoingMessage(sourceId, logAddress,
                        debug("{} {} - Successors after stabilization are {}", state.getSelfId(), sourceId, state.getSuccessors()));
                
                
                // notify it that we're its predecessor
                addOutgoingExternalMessage(
                        ctx,
                        successorAddress,
                        new NotifyRequest(selfId));
                ctx.addOutgoingMessage(sourceId, logAddress,
                        debug("{} {} - Notified {} that we're its successor", state.getSelfId(), sourceId, state.getSuccessor()));
            } catch (RuntimeException re) {
                ctx.addOutgoingMessage(sourceId, logAddress, warn("Failed to stabilize", re));
            }
        }
    }
    
    @Override
    public Address getId() {
        return sourceId;
    }

    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        new SleepSubcoroutine.Builder()
                .id(sourceId)
                .duration(duration)
                .timerAddressPrefix(timerAddress)
                .build()
                .run(cnt);
    }

    private <T> T funnelToRequestCoroutine(Continuation cnt, Address destination, Object message,
            Class<T> expectedResponseClass) throws Exception {
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .id(sourceId.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination)
                .request(message)
                .timerAddressPrefix(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
    
    private void addOutgoingExternalMessage(Context ctx, Address destination, Object message) {
        ctx.addOutgoingMessage(
                sourceId.appendSuffix(state.nextRandomId()),
                destination,
                message);
    }
}
