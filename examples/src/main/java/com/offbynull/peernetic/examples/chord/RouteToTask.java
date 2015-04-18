package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerResponse;
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
    private ExternalPointer currentNode;
    
    private NodeId foundId;
    private String foundAddress;
    
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
        
        LOG.debug("{} {} - Routing to {}", state.getSelfId(), sourceId, findId);
        
        Pointer initialPointer = state.getClosestFinger(findId);
        if (initialPointer instanceof InternalPointer) {
            // our finger table may be corrupt/incomplete, try with maximum non-base finger
            initialPointer = state.getMaximumNonSelfFinger();
            if (initialPointer == null) {
                // we don't have a maximum non-base, at this point we're fucked so just give back self
                foundId = state.getSelfId();
                LOG.debug("{} {} - Routing resulted in self {}", state.getSelfId(), sourceId, findId);
                return;
            }
        }

        currentNode = (ExternalPointer) initialPointer;
        NodeId skipId = state.getSelfId();
        
        // move forward until you can't move forward anymore
        while (true) {
            NodeId oldCurrentNodeId = currentNode.getId();

            LOG.debug("{} {} - Requesting closest finger to {} from {}", state.getSelfId(), sourceId, findId, currentNode);
            
            GetClosestFingerResponse gcpfr;
            try {
                gcpfr = funnelToRequestCoroutine(cnt,
                        currentNode.getAddress(),
                        new GetClosestFingerRequest(
                                state.generateExternalMessageId(),
                                findId,
                                skipId),
                        Duration.ofSeconds(10L),
                        GetClosestFingerResponse.class);
            } catch (RuntimeException re) {
                LOG.warn("{} {} - Routing failed -- failed to get closest finger from {}", state.getSelfId(), sourceId, currentNode);
                return;
            }

            String address = gcpfr.getAddress();
            NodeId id = gcpfr.getChordId();

            LOG.debug("{} {} - {} reported that its closest finger to {} is {}", state.getSelfId(), sourceId, currentNode, findId, id);
            
            if (address == null) {
                currentNode = new ExternalPointer(id, currentNode.getAddress());
            } else {
                currentNode = new ExternalPointer(id, address);
            }

            if (id.equals(oldCurrentNodeId) || id.equals(skipId)) {
                break;
            }
        }
        
        foundId = currentNode.getId();
        if (!currentNode.getId().equals(skipId)) {
            foundAddress = currentNode.getAddress();
        }
        
        LOG.debug("{} {} - {} routed to {} at address {}", state.getSelfId(), sourceId, currentNode, findId, foundId, foundAddress);
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
