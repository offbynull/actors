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
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import org.apache.commons.lang3.Validate;

final class RouteToSuccessorSubcoroutine implements Subcoroutine<Pointer> {
    
    private final Address sourceId;
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    
    private final NodeId findId;
    private Pointer found;

    public RouteToSuccessorSubcoroutine(Address sourceId, State state, Address timerAddress, Address logAddress, NodeId findId) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(findId);
        this.sourceId = sourceId;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.findId = findId;
    }
    
    @Override
    public Pointer run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Routing to predecessor of {}", state.getSelfId(), sourceId, findId));
        Pointer pointer = funnelToRouteToCoroutine(cnt, findId);
        if (pointer == null) {
            ctx.addOutgoingMessage(sourceId, logAddress,
                    debug("{} {} - Failed to route to predecessor of {}", state.getSelfId(), sourceId, findId));
            return null;
        }
        ctx.addOutgoingMessage(sourceId, logAddress,
                debug("{} {} - Predecessor of {} routed to {}", state.getSelfId(), sourceId, findId, pointer));
        
        if (pointer instanceof InternalPointer) {
            // If routed to self, return own successor
            Pointer successor = state.getSuccessor();
            
            // validate that successor is still alive (if external)
            if (successor instanceof ExternalPointer) {
                GetIdResponse gir = funnelToRequestCoroutine(
                        cnt,
                        ((ExternalPointer) successor).getAddress(),
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
            Address ptrAddress = ((ExternalPointer) pointer).getAddress();
            GetSuccessorResponse gsr = funnelToRequestCoroutine(
                        cnt,
                        ptrAddress,
                        new GetSuccessorRequest(),
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
                    new GetIdRequest(),
                    GetIdResponse.class);
            found = state.toPointer(gir.getChordId(), succAddress);
        } else {
            throw new IllegalArgumentException();
        }
        
        ctx.addOutgoingMessage(sourceId, logAddress,
                debug("{} {} - Successor of {} routed to {}", state.getSelfId(), sourceId, findId, found));
        return found;
    }
    
    @Override
    public Address getId() {
        return sourceId;
    }
    
    private Pointer funnelToRouteToCoroutine(Continuation cnt, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "routetopred" + state.nextRandomId();
        RouteToSubcoroutine innerCoroutine = new RouteToSubcoroutine(
                sourceId.appendSuffix(idSuffix),
                state,
                timerAddress,
                logAddress,
                findId);
        return innerCoroutine.run(cnt);
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, Address destination, Object message,
            Class<T> expectedResponseClass) throws Exception {
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
