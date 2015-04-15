package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RouteToPredecessorTask implements Coroutine {

    private static final Logger LOG = LoggerFactory.getLogger(RouteToPredecessorTask.class);

    private final NodeId findId;
    private ExternalPointer currentNode;

    private NodeId foundId;
    private String foundAddress;

    private final String sourceId;
    private final State state;

    public RouteToPredecessorTask(String sourceId, State state, NodeId findId) {
        Validate.notNull(sourceId);
        Validate.notNull(findId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.findId = findId;
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        NodeId selfId = state.getSelfId();

        Context ctx = (Context) cnt.getContext();
        
        LOG.debug("Routing to predecessor of {}", findId);

        try {
            if (findId.isWithin(selfId, false, state.getSuccessor().getId(), true)) {
                LOG.debug("Predecessor is between self and successor. Stopping.");
                foundId = selfId;
                foundAddress = null;
                return;
            }

            Pointer initialPointer = state.getClosestPrecedingFinger(findId);
            if (initialPointer instanceof InternalPointer) { // if the closest predecessor is yourself -- which means the ft is empty
                LOG.debug("Self is the closest preceding finger. Stopping.");
                foundId = selfId;
                foundAddress = null;
                return;
            } else if (initialPointer instanceof ExternalPointer) {
                currentNode = (ExternalPointer) initialPointer;
                LOG.debug("{} is the closest preceding finger on file.", currentNode);
            } else {
                throw new IllegalStateException();
            }

            // move forward until you can't move forward anymore
            while (true) {
                LOG.debug("Querying successor of {}.", currentNode.getId());
                
                GetSuccessorResponse gsr = funnelToRequestCoroutine(
                        cnt,
                        currentNode.getAddress(),
                        new GetSuccessorRequest(state.generateExternalMessageId()),
                        Duration.ofSeconds(10L),
                        GetSuccessorResponse.class);
                NodeId succId = state.toId(gsr.getEntries().get(0).getChordId());

                LOG.debug("Successor of {} is {}.", currentNode.getId(), succId);
                if (findId.isWithin(currentNode.getId(), false, succId, true)) {
                    LOG.debug("{} is between {} and {}.", findId, currentNode.getId(), succId);
                    break;
                }

                GetClosestPrecedingFingerResponse gcpfr = funnelToRequestCoroutine(
                        cnt,
                        currentNode.getAddress(),
                        new GetClosestPrecedingFingerRequest(state.generateExternalMessageId(), findId.getValueAsByteArray()),
                        Duration.ofSeconds(10L),
                        GetClosestPrecedingFingerResponse.class);
                String address = gcpfr.getAddress();
                NodeId id = state.toId(gcpfr.getChordId());

                if (address == null) {
                    currentNode = new ExternalPointer(id, currentNode.getAddress());
                } else {
                    currentNode = new ExternalPointer(id, address);
                }

                LOG.debug("{} reports its closest predecessor is {}.", succId, id);
            }

            foundId = currentNode.getId();
            if (!currentNode.getId().equals(selfId)) {
                foundAddress = currentNode.getAddress();
            }

            LOG.debug("Found {} at {}.", foundId, foundAddress);
        } catch (RuntimeException re) {
            LOG.error(re.getMessage());
        }
    }

    public Pointer getResult() {
        if (foundId == null) {
            return null;
        }

        if (state.isSelfId(foundId)) {
            return new InternalPointer(foundId);
        }

        return new ExternalPointer(foundId, foundAddress);
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
}
