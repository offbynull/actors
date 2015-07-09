package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
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
import com.offbynull.peernetic.examples.chord.model.NodeId;
import org.apache.commons.lang3.Validate;

final class InitRouteToSuccessorSubcoroutine implements Subcoroutine<Pointer> {

    private final Address sourceId;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    private final ExternalPointer bootstrapNode;
    private final NodeId findId;

    public InitRouteToSuccessorSubcoroutine(Address sourceId, State state, Address timerAddress, Address logAddress, ExternalPointer bootstrapNode,
            NodeId findId) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);
        this.sourceId = sourceId;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.bootstrapNode = bootstrapNode;
        this.findId = findId;
    }
    
    @Override
    public Pointer run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Routing to predecessor of {}", state.getSelfId(), sourceId, findId));
        ExternalPointer pointer = funnelToInitRouteToCoroutine(cnt, bootstrapNode, findId);
        if (pointer == null) {
            ctx.addOutgoingMessage(sourceId, logAddress,
                    debug("{} {} - Failed to route to predecessor of {}", state.getSelfId(), sourceId, findId));
            return null;
        }
        
        ctx.addOutgoingMessage(sourceId, logAddress,
                debug("{} {} - Predecessor of {} routed to {}", state.getSelfId(), sourceId, findId, pointer));
        String ptrLinkId = ((ExternalPointer) pointer).getLinkId();
        GetSuccessorResponse gsr = funnelToRequestCoroutine(
                    cnt,
                    ptrLinkId,
                    new GetSuccessorRequest(),
                    GetSuccessorResponse.class);

        SuccessorEntry successorEntry = gsr.getEntries().get(0);

        String succLinkId;
        if (successorEntry instanceof InternalSuccessorEntry) { // this means the successor to the node is itself
            succLinkId = ptrLinkId;
        } else if (successorEntry instanceof ExternalSuccessorEntry) {
            succLinkId = ((ExternalSuccessorEntry) successorEntry).getLinkId();
        } else {
            throw new IllegalStateException();
        }

        // ask for that successor's id, wait for response here
        GetIdResponse gir = funnelToRequestCoroutine(
                cnt,
                succLinkId,
                new GetIdRequest(),
                GetIdResponse.class);
        Pointer found = state.toPointer(gir.getChordId(), succLinkId);

        ctx.addOutgoingMessage(sourceId, logAddress,
                debug("{} {} - Successor of {} routed to {}", state.getSelfId(), sourceId, findId, found));
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
        
        String idSuffix = "initroutetopred" + state.nextRandomId();
        InitRouteToCoroutine innerCoroutine = new InitRouteToCoroutine(
                sourceId.appendSuffix(idSuffix),
                state,
                timerAddress,
                logAddress,
                bootstrapNode,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass) throws Exception {
        Address destination = state.getAddressTransformer().linkIdToRemoteAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .id(sourceId.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination.appendSuffix("router", "handler"))
                .request(message)
                .timerAddressPrefix(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
