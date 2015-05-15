package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UpdateOthersTask implements Subcoroutine<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateOthersTask.class);

    private final Address sourceId;
    private final State state;

    public UpdateOthersTask(Address sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
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
                
                LOG.debug("{} {} - Attempting to find {} and inform it that we are one if it's fingers ({}}", state.getSelfId(), sourceId,
                        routerId, i);

                Pointer foundRouter;
                try {
                    foundRouter = funnelToRouteToCoroutine(cnt, routerId);
                } catch (RuntimeException re) {
                    LOG.warn("{} {} - Unable to route to predecessor: {}", state.getSelfId(), sourceId, re);
                    continue;
                }
                
                if (foundRouter == null) {
                    LOG.warn("{} {} - Route to predecessor failed", state.getSelfId(), sourceId);
                    continue;
                }

                if (foundRouter instanceof ExternalPointer) {
                    LOG.debug("{} {} - Asking {} to put us in to its finger table", state.getSelfId(), sourceId, foundRouter);
                    addOutgoingExternalMessage(ctx,
                            ((ExternalPointer) foundRouter).getAddress(),
                            new UpdateFingerTableRequest(state.generateExternalMessageId(), selfId));
                } else if (foundRouter instanceof InternalPointer) {
                    ExternalPointer pred = state.getPredecessor();
                    if (pred != null) {
                        LOG.debug("{} {} - {} routed to self, notifying predecessor {}", state.getSelfId(), sourceId, pred);
                        addOutgoingExternalMessage(ctx,
                                pred.getAddress(),
                                new UpdateFingerTableRequest(state.generateExternalMessageId(), selfId));
                    } else {
                        LOG.debug("{} {} - {} routed to self, but no predecessor to notify, so skipping", state.getSelfId(), sourceId);
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

        String idSuffix = "routeto" + state.generateExternalMessageId();
        RouteToTask innerCoroutine = new RouteToTask(
                sourceId.appendSuffix(idSuffix),
                state,
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
                .timerAddressPrefix(state.getTimerPrefix())
                .build()
                .run(cnt);
    }

    private void addOutgoingExternalMessage(Context ctx, Address destination, ExternalMessage message) {
        Validate.notNull(ctx);
        Validate.notNull(destination);
        Validate.notNull(message);

        ctx.addOutgoingMessage(
                sourceId.appendSuffix("" + message.getId()),
                destination,
                message);
    }
}
