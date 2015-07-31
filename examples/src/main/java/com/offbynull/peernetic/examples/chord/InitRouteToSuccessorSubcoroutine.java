package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
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

    private final Address subAddress;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    private final ExternalPointer bootstrapNode;
    private final NodeId findId;

    public InitRouteToSuccessorSubcoroutine(Address subAddress, State state, ExternalPointer bootstrapNode,
            NodeId findId) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.bootstrapNode = bootstrapNode;
        this.findId = findId;
    }
    
    @Override
    public Pointer run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Routing to predecessor of {}", state.getSelfId(), subAddress, findId));
        ExternalPointer pointer = funnelToInitRouteToCoroutine(cnt, bootstrapNode, findId);
        if (pointer == null) {
            ctx.addOutgoingMessage(subAddress, logAddress,
                    debug("{} {} - Failed to route to predecessor of {}", state.getSelfId(), subAddress, findId));
            return null;
        }
        
        ctx.addOutgoingMessage(subAddress, logAddress,
                debug("{} {} - Predecessor of {} routed to {}", state.getSelfId(), subAddress, findId, pointer));
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

        // ask for that successor's address, wait for response here
        GetIdResponse gir = funnelToRequestCoroutine(
                cnt,
                succLinkId,
                new GetIdRequest(),
                GetIdResponse.class);
        Pointer found = state.toPointer(gir.getChordId(), succLinkId);

        ctx.addOutgoingMessage(subAddress, logAddress,
                debug("{} {} - Successor of {} routed to {}", state.getSelfId(), subAddress, findId, found));
        return found;
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    private ExternalPointer funnelToInitRouteToCoroutine(Continuation cnt, ExternalPointer bootstrapNode, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(bootstrapNode);
        Validate.notNull(findId);
        
        String idSuffix = "initroutetopred" + state.nextRandomId();
        InitRouteToCoroutine innerCoroutine = new InitRouteToCoroutine(
                subAddress.appendSuffix(idSuffix),
                state,
                bootstrapNode,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass) throws Exception {
        Address destination = state.getAddressTransformer().linkIdToRemoteAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .address(subAddress.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                .request(message)
                .timerAddress(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
