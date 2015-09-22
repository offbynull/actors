package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class StabilizeSubcoroutine implements Subcoroutine<Void> {
    
    private final Address subAddress;
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    private final IdGenerator idGenerator;

    public StabilizeSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.idGenerator = state.getIdGenerator();
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        NodeId selfId = state.getSelfId();
        
        Context ctx = (Context) cnt.getContext();
        
        while (true) {
            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
            
            Pointer successor = state.getSuccessor();
            if (state.isSelfId(successor.getId())) {
                continue;
            }
            
            // ask for successor's pred
            String successorLinkId = ((ExternalPointer) successor).getLinkId();
            GetPredecessorResponse gpr;
            try {
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Requesting successor's ({}) predecessor", state.getSelfId(), subAddress, successor));
                
                gpr = funnelToRequestCoroutine(
                        cnt,
                        successorLinkId,
                        new GetPredecessorRequest(),
                        GetPredecessorResponse.class,
                        true);
                
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Successor's ({}) predecessor is {}", state.getSelfId(), subAddress, successor, gpr.getChordId()));
            } catch (Exception e) {
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Successor did not respond as expected, moving to next successor", state.getSelfId(), subAddress));
                state.moveToNextSuccessor();
                continue;
            }

            try {
                // check to see if predecessor is between us and our successor
                if (gpr.getChordId() != null) {
                    String linkId = gpr.getLinkId();
                    NodeId potentiallyNewSuccessorId = gpr.getChordId();
                    NodeId existingSuccessorId = ((ExternalPointer) successor).getId();

                    if (potentiallyNewSuccessorId.isWithin(selfId, false, existingSuccessorId, false)) {
                        // it is between us and our successor, so update
                        ExternalPointer newSuccessor = new ExternalPointer(potentiallyNewSuccessorId, linkId);

                        successor = newSuccessor;
                        successorLinkId = newSuccessor.getLinkId();
                    }
                }
                
                // successor may have been updated by block above
                // ask successor for its successors
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Requesting successor's ({}) successor", state.getSelfId(), subAddress, successor));
                GetSuccessorResponse gsr = funnelToRequestCoroutine(
                        cnt,
                        successorLinkId,
                        new GetSuccessorRequest(),
                        GetSuccessorResponse.class,
                        true);
                
                List<Pointer> subsequentSuccessors = new ArrayList<>();
                gsr.getEntries().stream().map(x -> {
                    NodeId id = x.getChordId();

                    if (x instanceof InternalSuccessorEntry) {
                        return new InternalPointer(id);
                    } else if (x instanceof ExternalSuccessorEntry) {
                        return new ExternalPointer(id, ((ExternalSuccessorEntry) x).getLinkId());
                    } else {
                        throw new IllegalStateException();
                    }
                }).forEachOrdered(x -> subsequentSuccessors.add(x));
                
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Successor's ({}) successor is {}", state.getSelfId(), subAddress, successor, subsequentSuccessors));

                // mark it as our new successor
                state.updateSuccessor((ExternalPointer) successor, subsequentSuccessors);
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Successors after stabilization are {}", state.getSelfId(), subAddress, state.getSuccessors()));
                
                
                // notify it that we're its predecessor
                funnelToRequestCoroutine(
                        cnt,
                        successorLinkId,
                        new NotifyRequest(selfId),
                        NotifyResponse.class,
                        false);
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Notified {} that we're its successor", state.getSelfId(), subAddress, state.getSuccessor()));
            } catch (RuntimeException re) {
                ctx.addOutgoingMessage(subAddress, logAddress, warn("Failed to stabilize", re));
            }
        }
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        new SleepSubcoroutine.Builder()
                .sourceAddress(subAddress)
                .duration(duration)
                .timerAddress(timerAddress)
                .build()
                .run(cnt);
    }

    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass, boolean exceptionOnBadResponse) throws Exception {
        Address destination = state.getAddressTransformer().toAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .sourceAddress(subAddress, idGenerator)
                .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                .request(message)
                .timerAddress(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .throwExceptionIfNoResponse(exceptionOnBadResponse)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
