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
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RouteToTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(RouteToTask.class);

    private final NodeId findId;
    
    private Pointer foundPointer;
    
    private final String sourceId;
    private final State state;

    public RouteToTask(String sourceId, State state, NodeId findId) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(findId);
        this.sourceId = sourceId;
        this.state = state;
        this.findId = findId;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        NodeId selfId = state.getSelfId();
        
        LOG.debug("{} {} - Routing to {}", state.getSelfId(), sourceId, findId);
        
        
        Pointer currentNode = state.getSelfPointer();
        while (true) {
            LOG.debug("{} {} - Search for {} moving forward to {}", state.getSelfId(), sourceId, findId, currentNode);
            
            NodeId successorId;
            if (currentNode instanceof InternalPointer) {
                successorId = state.getSuccessor().getId();
                
                if (findId.isWithin(currentNode.getId(), false, successorId, true) ||
                        successorId.equals(currentNode.getId())) {
                    foundPointer = currentNode;
                    break;
                }

                currentNode = state.getClosestPrecedingFinger(findId);
            } else if (currentNode instanceof ExternalPointer) {
                GetSuccessorResponse gsr;
                try {
                    gsr = funnelToRequestCoroutine(
                                cnt,
                                ((ExternalPointer) currentNode).getAddress(),
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
                            ((ExternalPointer)currentNode).getAddress(),
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
                ExternalPointer newNode = state.toExternalPointer(gcpfr.getChordId(), gcpfr.getAddress(),
                        ((ExternalPointer) currentNode).getAddress());
                if (newNode.equals(currentNode)) {
                    foundPointer = newNode;
                    break;
                }

                currentNode = newNode;
            }
        }

        
        LOG.debug("{} {} - Routing to {} resulted in {}", state.getSelfId(), sourceId, findId, foundPointer);
    }

    public Pointer getResult() {
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
