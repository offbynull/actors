package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RouteToSuccessorTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(RouteToSuccessorTask.class);

    private final NodeId findId;
    
    private Pointer found;
    
    private final String sourceId;
    private final State state;

    public RouteToSuccessorTask(String sourceId, State state, NodeId findId) {
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
        
        LOG.debug("{} {} - Routing to predecessor of {}", state.getSelfId(), sourceId, findId);
        Pointer pointer = funnelToRouteToCoroutine(cnt, findId);
        if (pointer == null) {
            LOG.debug("{} {} - Failed to route to predecessor of {}", state.getSelfId(), sourceId, findId);
            return;
        }
        LOG.debug("{} {} - Predecessor of {} routed to {}", state.getSelfId(), sourceId, findId, pointer);
        
        if (pointer instanceof InternalPointer) {
            // If routed to self, return own successor
            Pointer successor = state.getSuccessor();
            
            // validate that successor is still alive (if external)
            if (successor instanceof ExternalPointer) {
                GetIdResponse gir = funnelToRequestCoroutine(
                        cnt,
                        ((ExternalPointer) successor).getAddress(),
                        new GetIdRequest(state.generateExternalMessageId()),
                        Duration.ofSeconds(10L),
                        GetIdResponse.class);
                found = successor;
            } else if (successor instanceof InternalPointer) {
                found = successor;
            } else {
                throw new IllegalStateException();
            }
        } else if (pointer instanceof ExternalPointer) {
            // If routed to external node, ask external node for successor details
            String ptrAddress = ((ExternalPointer) pointer).getAddress();
            GetSuccessorResponse gsr = funnelToRequestCoroutine(
                        cnt,
                        ptrAddress,
                        new GetSuccessorRequest(state.generateExternalMessageId()),
                        Duration.ofSeconds(10L),
                        GetSuccessorResponse.class);
            
            SuccessorEntry successorEntry = gsr.getEntries().get(0);

            String succAddress;
            if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
                succAddress = ptrAddress;
            } else if (successorEntry instanceof ExternalSuccessorEntry) {
                succAddress = ((ExternalSuccessorEntry) successorEntry).getAddress();
            } else {
                throw new IllegalStateException();
            }

            // ask for that successor's id, wait for response here
            GetIdResponse gir = funnelToRequestCoroutine(
                    cnt,
                    succAddress,
                    new GetIdRequest(state.generateExternalMessageId()),
                    Duration.ofSeconds(10L),
                    GetIdResponse.class);
            found = state.toPointer(gir.getChordId(), succAddress);
        } else {
            throw new IllegalArgumentException();
        }
        
        LOG.debug("{} {} - Successor of {} routed to {}", state.getSelfId(), sourceId, findId, found);
    }
    
    public Pointer getResult() {
        return found;
    }
    
    private Pointer funnelToRouteToCoroutine(Continuation cnt, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "routetopred" + state.generateExternalMessageId();
        RouteToTask innerCoroutine = new RouteToTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
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
