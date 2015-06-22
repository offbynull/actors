package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class UpdateOthersSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final State state;
    private final Address logAddress;
    private final Address timerAddress;

    public UpdateOthersSubcoroutine(Address sourceId, State state, Address timerAddress, Address logAddress) {
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

        int maxIdx = state.getFingerTableLength(); // bit length of ring
        
        while (true) {
            for (int i = 0; i < maxIdx; i++) {
                // get id of node that should have us in its finger table at index i
                NodeId routerId = state.getIdThatShouldHaveThisNodeAsFinger(i);
                
                ctx.addOutgoingMessage(sourceId, logAddress,
                        debug("{} {} - Attempting to find {} and inform it that we are one if it's fingers ({}}",
                                state.getSelfId(),
                                sourceId,
                                routerId,
                                i));

                Pointer foundRouter;
                try {
                    foundRouter = funnelToRouteToCoroutine(cnt, routerId);
                } catch (RuntimeException re) {
                    ctx.addOutgoingMessage(sourceId, logAddress,
                            warn("{} {} - Unable to route to predecessor: {}", state.getSelfId(), sourceId, re));
                    continue;
                }
                
                if (foundRouter == null) {
                    ctx.addOutgoingMessage(sourceId, logAddress,
                            warn("{} {} - Route to predecessor failed", state.getSelfId(), sourceId));
                    continue;
                }

                if (foundRouter instanceof ExternalPointer) {
                    ctx.addOutgoingMessage(sourceId, logAddress,
                            debug("{} {} - Asking {} to put us in to its finger table", state.getSelfId(), sourceId, foundRouter));
                    funnelToRequestCoroutine(cnt,
                            ((ExternalPointer) foundRouter).getAddress(),
                            new UpdateFingerTableRequest(selfId),
                            UpdateFingerTableResponse.class,
                            false);
                } else if (foundRouter instanceof InternalPointer) {
                    ExternalPointer pred = state.getPredecessor();
                    if (pred != null) {
                        ctx.addOutgoingMessage(sourceId, logAddress,
                                debug("{} {} - {} routed to self, notifying predecessor {}", state.getSelfId(), sourceId, pred));
                        funnelToRequestCoroutine(cnt,
                                pred.getAddress(),
                                new UpdateFingerTableRequest(selfId),
                                UpdateFingerTableResponse.class,
                                false);
                    } else {
                        ctx.addOutgoingMessage(sourceId, logAddress,
                                debug("{} {} - {} routed to self, but no predecessor to notify, so skipping", state.getSelfId(), sourceId));
                    }
                } else {
                    throw new IllegalStateException();
                }
            }

            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
        }
    }

    @Override
    public Address getId() {
        return sourceId;
    }
    
    private Pointer funnelToRouteToCoroutine(Continuation cnt, NodeId routerId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(routerId);

        String idSuffix = "routeto" + state.nextRandomId();
        RouteToSubcoroutine innerCoroutine = new RouteToSubcoroutine(
                sourceId.appendSuffix(idSuffix),
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
                .id(sourceId)
                .duration(duration)
                .timerAddressPrefix(timerAddress)
                .build()
                .run(cnt);
    }

    private <T> T funnelToRequestCoroutine(Continuation cnt, Address destination, Object message, Class<T> expectedResponseClass,
            boolean exceptionOnBadResponse) throws Exception {
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .id(sourceId.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination)
                .request(message)
                .timerAddressPrefix(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .throwExceptionIfNoResponse(exceptionOnBadResponse)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
