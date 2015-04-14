package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import com.offbynull.peernetic.examples.common.coroutines.SendRequestCoroutine;
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
        
        Context ctx = (Context) cnt.getContext();
        ParentCoroutine parentCoroutine = new ParentCoroutine(sourceId, state.getTimerPrefix(), ctx);
        
        Pointer initialPointer = state.getClosestFinger(findId);
        if (initialPointer instanceof InternalPointer) {
            // our finger table may be corrupt/incomplete, try with maximum non-base finger
            initialPointer = state.getMaximumNonSelfFinger();
            if (initialPointer == null) {
                // we don't have a maximum non-base, at this point we're fucked so just give back self
                foundId = state.getSelfId();
                return;
            }
        }

        currentNode = (ExternalPointer) initialPointer;
        NodeId skipId = state.getSelfId();
        
        // move forward until you can't move forward anymore
        while (true) {
            NodeId oldCurrentNodeId = currentNode.getId();

            GetClosestFingerResponse gcpfr;
            try {
                long msgId;
                SendRequestCoroutine sendRequestCoroutine;
                
                msgId = state.generateExternalMessageId();
                sendRequestCoroutine = parentCoroutine.addSendRequest(
                        currentNode.getAddress(),
                        new GetClosestFingerRequest(msgId, findId.getValueAsByteArray(), skipId.getValueAsByteArray()),
                        Duration.ofSeconds(10L),
                        GetClosestFingerResponse.class);
                parentCoroutine.run(cnt);
                gcpfr = sendRequestCoroutine.getResponse();
            } catch (RuntimeException re) {
                LOG.warn("Routing failed -- failed to get closest finger from {}", currentNode);
                return;
            }

            String address = gcpfr.getAddress();
            NodeId id = state.toId(gcpfr.getChordId());

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
}
