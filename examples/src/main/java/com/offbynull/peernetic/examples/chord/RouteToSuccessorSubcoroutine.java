package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
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
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import org.apache.commons.lang3.Validate;

final class RouteToSuccessorSubcoroutine implements Subcoroutine<Pointer> {
    
    private final Address subAddress;
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    private final IdGenerator idGenerator;
    
    private final NodeId findId;
    private Pointer found;

    public RouteToSuccessorSubcoroutine(Address subAddress, State state, NodeId findId) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(findId);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.idGenerator = state.getIdGenerator();
        this.findId = findId;
    }
    
    @Override
    public Pointer run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Routing to predecessor of {}", state.getSelfId(), subAddress, findId));
        Pointer pointer = funnelToRouteToCoroutine(cnt, findId);
        if (pointer == null) {
            ctx.addOutgoingMessage(subAddress, logAddress,
                    debug("{} {} - Failed to route to predecessor of {}", state.getSelfId(), subAddress, findId));
            return null;
        }
        ctx.addOutgoingMessage(subAddress, logAddress,
                debug("{} {} - Predecessor of {} routed to {}", state.getSelfId(), subAddress, findId, pointer));
        
        if (pointer instanceof InternalPointer) {
            // If routed to self, return own successor
            Pointer successor = state.getSuccessor();
            
            // validate that successor is still alive (if external)
            if (successor instanceof ExternalPointer) {
                GetIdResponse gir = funnelToRequestCoroutine(
                        cnt,
                        ((ExternalPointer) successor).getLinkId(),
                        new GetIdRequest(),
                        GetIdResponse.class);
                found = successor;
            } else if (successor instanceof InternalPointer) {
                found = successor;
            } else {
                throw new IllegalStateException();
            }
        } else if (pointer instanceof ExternalPointer) {
            // If routed to external node, ask external node for successor details
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
            found = state.toPointer(gir.getChordId(), succLinkId);
        } else {
            throw new IllegalArgumentException();
        }
        
        ctx.addOutgoingMessage(subAddress, logAddress,
                debug("{} {} - Successor of {} routed to {}", state.getSelfId(), subAddress, findId, found));
        return found;
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    private Pointer funnelToRouteToCoroutine(Continuation cnt, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "routetopred" + idGenerator.generate();
        RouteToSubcoroutine innerCoroutine = new RouteToSubcoroutine(
                subAddress.appendSuffix(idSuffix),
                state,
                findId);
        return innerCoroutine.run(cnt);
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass) throws Exception {
        Address destination = state.getAddressTransformer().linkIdToRemoteAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .sourceAddress(subAddress, idGenerator)
                .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                .request(message)
                .timerAddress(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
