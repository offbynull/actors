package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class UpdateOthersSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final State state;
    private final Address logAddress;
    private final Address timerAddress;

    public UpdateOthersSubcoroutine(Address subAddress, State state, Address timerAddress, Address logAddress) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        NodeId selfId = state.getSelfId();

        Context ctx = (Context) cnt.getContext();

        int maxIdx = state.getFingerTableLength(); // bit length of ring
        
        while (true) {
            for (int i = 0; i < maxIdx; i++) {
                // get address of node that should have us in its finger table at index i
                NodeId routerId = state.getIdThatShouldHaveThisNodeAsFinger(i);
                
                ctx.addOutgoingMessage(subAddress, logAddress,
                        debug("{} {} - Attempting to find {} and inform it that we are one if it's fingers ({}}",
                                state.getSelfId(),
                                subAddress,
                                routerId,
                                i));

                Pointer foundRouter;
                try {
                    foundRouter = funnelToRouteToCoroutine(cnt, routerId);
                } catch (RuntimeException re) {
                    ctx.addOutgoingMessage(subAddress, logAddress,
                            warn("{} {} - Unable to route to predecessor: {}", state.getSelfId(), subAddress, re));
                    continue;
                }
                
                if (foundRouter == null) {
                    ctx.addOutgoingMessage(subAddress, logAddress,
                            warn("{} {} - Route to predecessor failed", state.getSelfId(), subAddress));
                    continue;
                }

                if (foundRouter instanceof ExternalPointer) {
                    ctx.addOutgoingMessage(subAddress, logAddress,
                            debug("{} {} - Asking {} to put us in to its finger table", state.getSelfId(), subAddress, foundRouter));
                    funnelToRequestCoroutine(cnt,
                            ((ExternalPointer) foundRouter).getLinkId(),
                            new UpdateFingerTableRequest(selfId),
                            UpdateFingerTableResponse.class,
                            false);
                } else if (foundRouter instanceof InternalPointer) {
                    ExternalPointer pred = state.getPredecessor();
                    if (pred != null) {
                        ctx.addOutgoingMessage(subAddress, logAddress,
                                debug("{} {} - {} routed to self, notifying predecessor {}", state.getSelfId(), subAddress, pred));
                        funnelToRequestCoroutine(cnt,
                                pred.getLinkId(),
                                new UpdateFingerTableRequest(selfId),
                                UpdateFingerTableResponse.class,
                                false);
                    } else {
                        ctx.addOutgoingMessage(subAddress, logAddress,
                                debug("{} {} - {} routed to self, but no predecessor to notify, so skipping", state.getSelfId(), subAddress));
                    }
                } else {
                    throw new IllegalStateException();
                }
            }

            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
        }
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    private Pointer funnelToRouteToCoroutine(Continuation cnt, NodeId routerId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(routerId);

        String idSuffix = "routeto" + state.nextRandomId();
        RouteToSubcoroutine innerCoroutine = new RouteToSubcoroutine(
                subAddress.appendSuffix(idSuffix),
                state,
                timerAddress,
                logAddress,
                routerId);
        return innerCoroutine.run(cnt);
    }

    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());

        new SleepSubcoroutine.Builder()
                .address(subAddress)
                .duration(duration)
                .timerAddressPrefix(timerAddress)
                .build()
                .run(cnt);
    }

    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass, boolean exceptionOnBadResponse) throws Exception {
        Address destination = state.getAddressTransformer().linkIdToRemoteAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .address(subAddress.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                .request(message)
                .timerAddressPrefix(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .throwExceptionIfNoResponse(exceptionOnBadResponse)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
