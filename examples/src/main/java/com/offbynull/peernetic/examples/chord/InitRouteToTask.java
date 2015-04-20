package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// unique to initialization phase in that it doesn't consider you as a node in the network (you're initializing, you haven't connected yet)
final class InitRouteToTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(InitRouteToTask.class);

    private final ExternalPointer bootstrapNode;
    private final NodeId findId;

    private ExternalPointer foundPointer;
    
    private final String sourceId;
    private final State state;

    public InitRouteToTask(String sourceId, State state, ExternalPointer bootstrapNode, NodeId findId) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);
        this.sourceId = sourceId;
        this.state = state;
        this.findId = findId;
        this.bootstrapNode = bootstrapNode;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        LOG.debug("{} {} - Routing to {}", state.getSelfId(), sourceId, findId);
        
        
        ExternalPointer currentNode = bootstrapNode;
        while (true) {
            NodeId successorId;
            GetSuccessorResponse gsr;
            try {
                gsr = funnelToRequestCoroutine(
                            cnt,
                            currentNode.getAddress(),
                            new GetSuccessorRequest(state.generateExternalMessageId()),
                            Duration.ofSeconds(10L),
                            GetSuccessorResponse.class);
                successorId = gsr.getEntries().get(0).getChordId();
            } catch (RuntimeException re) {
                LOG.warn("{} {} - Routing failed -- failed to get successor from {}", state.getSelfId(), sourceId, currentNode);
                return;
            }

            if (findId.isWithin(currentNode.getId(), false, successorId, true) ||
                    successorId.equals(currentNode.getId())) {
                foundPointer = currentNode;
                break;
            }

            GetClosestPrecedingFingerResponse gcpfr;
            try {
                gcpfr = funnelToRequestCoroutine(cnt,
                        currentNode.getAddress(),
                        new GetClosestPrecedingFingerRequest(
                                state.generateExternalMessageId(),
                                findId),
                        Duration.ofSeconds(10L),
                        GetClosestPrecedingFingerResponse.class);
            } catch (RuntimeException re) {
                LOG.warn("{} {} - Routing failed -- failed to get closest finger from {}", state.getSelfId(), sourceId, currentNode);
                return;
            }

                // special case -- if node we're querying returns itself for closest preceding finger, it means that its finger table its
            // empty (it is likely the first node making up the network). As such, if self, mark as found and return
            ExternalPointer newNode = state.toExternalPointer(gcpfr.getChordId(), gcpfr.getAddress(), currentNode.getAddress());
            if (newNode.equals(currentNode)) {
                foundPointer = newNode;
                break;
            }
            
            currentNode = newNode;
        }

        
        LOG.debug("{} {} - Routing to {} resulting in {} at {}", state.getSelfId(), sourceId, findId, foundPointer);
    }

    public ExternalPointer getResult() {
        return foundPointer;
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
