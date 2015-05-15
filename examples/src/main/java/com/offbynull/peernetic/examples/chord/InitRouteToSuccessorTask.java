package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.ExternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.InternalSuccessorEntry;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse.SuccessorEntry;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InitRouteToSuccessorTask implements Subcoroutine<Pointer> {
    
    private static final Logger LOG = LoggerFactory.getLogger(InitRouteToSuccessorTask.class);

    private final ExternalPointer bootstrapNode;
    private final NodeId findId;
    
    private final Address sourceId;
    private final State state;

    public InitRouteToSuccessorTask(Address sourceId, State state, ExternalPointer bootstrapNode, NodeId findId) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);
        this.sourceId = sourceId;
        this.state = state;
        this.bootstrapNode = bootstrapNode;
        this.findId = findId;
    }
    
    @Override
    public Pointer run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        LOG.debug("{} {} - Routing to predecessor of {}", state.getSelfId(), sourceId, findId);
        ExternalPointer pointer = funnelToInitRouteToCoroutine(cnt, bootstrapNode, findId);
        if (pointer == null) {
            LOG.debug("{} {} - Failed to route to predecessor of {}", state.getSelfId(), sourceId, findId);
            return null;
        }
        
        LOG.debug("{} {} - Predecessor of {} routed to {}", state.getSelfId(), sourceId, findId, pointer);
        Address ptrAddress = ((ExternalPointer) pointer).getAddress();
        GetSuccessorResponse gsr = funnelToRequestCoroutine(
                    cnt,
                    ptrAddress,
                    new GetSuccessorRequest(state.generateExternalMessageId()),
                    GetSuccessorResponse.class);

        SuccessorEntry successorEntry = gsr.getEntries().get(0);

        Address succAddress;
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
                GetIdResponse.class);
        Pointer found = state.toPointer(gir.getChordId(), succAddress);
        
        LOG.debug("{} {} - Successor of {} routed to {}", state.getSelfId(), sourceId, findId, found);
        return found;
    }
    
    @Override
    public Address getId() {
        return sourceId;
    }
    
    private ExternalPointer funnelToInitRouteToCoroutine(Continuation cnt, ExternalPointer bootstrapNode, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);
        
        String idSuffix = "initroutetopred" + state.generateExternalMessageId();
        InitRouteToTask innerCoroutine = new InitRouteToTask(
                sourceId.appendSuffix(idSuffix),
                state,
                bootstrapNode,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
    
    private <T extends ExternalMessage> T funnelToRequestCoroutine(Continuation cnt, Address destination, ExternalMessage message,
            Class<T> expectedResponseClass) throws Exception {
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .id(sourceId.appendSuffix("" + message.getId()))
                .destinationAddress(destination)
                .request(message)
                .timerAddressPrefix(state.getTimerPrefix())
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
