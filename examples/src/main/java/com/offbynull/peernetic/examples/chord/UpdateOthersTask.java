package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UpdateOthersTask implements Coroutine {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateOthersTask.class);

    private final String sourceId;
    private final State state;

    public UpdateOthersTask(String sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        NodeId selfId = state.getSelfId();
        
        Context ctx = (Context) cnt.getContext();
        ParentCoroutine parentCoroutine = new ParentCoroutine(sourceId, state.getTimerPrefix(), ctx);

        while (true) {
            long uniqueExtPtrCount = state.getFingers().stream()
                    .distinct()
                    .filter(x -> x instanceof ExternalPointer)
                    .count();
            if (uniqueExtPtrCount == 0L) {
                // nothing to update here
                return;
            } else if (uniqueExtPtrCount == 1L) {
                // special case not handled in chord paper's pseudo code
                //
                // if connecting to a overlay of size 1, find_predecessor() will always return yourself, so the node in the overlay will
                // never get your request to update its finger table and you will never be recognized
                ExternalPointer ptr = (ExternalPointer) state.getSuccessor();

                long msgId = state.generateExternalMessageId();
                ctx.addOutgoingMessage(
                        AddressUtils.parentize(sourceId, "" + msgId),
                        ptr.getAddress(),
                        new UpdateFingerTableRequest(msgId, selfId.getValueAsByteArray()));
            } else {
                int maxIdx = state.getFingerTableLength(); // bit length of ring
                for (int i = 0; i < maxIdx; i++) {
                    // get id of node that should have us in its finger table at index i
                    NodeId routerId = state.getIdThatShouldHaveThisNodeAsFinger(i);

                    Pointer foundRouter;
                    try{
                        String idSuffix = "" + state.generateExternalMessageId();
                        RouteToPredecessorTask routeToPredecessorTask = new RouteToPredecessorTask(
                                AddressUtils.parentize(sourceId, idSuffix),
                                state,
                                routerId);
                        parentCoroutine.add(idSuffix, routeToPredecessorTask);
                        parentCoroutine.run(cnt);
                        foundRouter = routeToPredecessorTask.getResult();
                    } catch (RuntimeException re) {
                        LOG.warn("Unable to route to predecessor", re);
                        continue;
                    }

                    if (foundRouter instanceof ExternalPointer) {
                        long msgId = state.generateExternalMessageId();
                        ctx.addOutgoingMessage(
                                AddressUtils.parentize(sourceId, "" + msgId),
                                ((ExternalPointer) foundRouter).getAddress(),
                                new UpdateFingerTableRequest(msgId, selfId.getValueAsByteArray()));
                    }
                }
            }

            parentCoroutine.addSleep(Duration.ofSeconds(1L));
            parentCoroutine.run(cnt);
        }
    }
}
