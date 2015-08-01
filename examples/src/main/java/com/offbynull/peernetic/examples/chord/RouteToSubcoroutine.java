package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import org.apache.commons.lang3.Validate;

final class RouteToSubcoroutine implements Subcoroutine<Pointer> {

    private final Address subAddress;
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    
    private final NodeId findId;

    public RouteToSubcoroutine(Address subAddress, State state, NodeId findId) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(findId);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.findId = findId;
    }

    @Override
    public Pointer run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        NodeId selfId = state.getSelfId();
        
        ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Routing to {}", state.getSelfId(), subAddress, findId));
        
        
        Pointer foundPointer;
        Pointer currentNode = state.getSelfPointer();
        while (true) {
            ctx.addOutgoingMessage(subAddress, logAddress,
                    debug("{} {} - Search for {} moving forward to {}", state.getSelfId(), subAddress, findId, currentNode));
            
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
                                ((ExternalPointer) currentNode).getLinkId(),
                                new GetSuccessorRequest(),
                                GetSuccessorResponse.class);
                    successorId = gsr.getEntries().get(0).getChordId();
                } catch (RuntimeException re) {
                    ctx.addOutgoingMessage(subAddress, logAddress,
                            warn("{} {} - Routing failed -- failed to get successor from {}", state.getSelfId(), subAddress, currentNode));
                    return null;
                }
                
                if (findId.isWithin(currentNode.getId(), false, successorId, true) ||
                        successorId.equals(currentNode.getId())) {
                    foundPointer = currentNode;
                    break;
                }
                
                GetClosestPrecedingFingerResponse gcpfr;
                try {
                    gcpfr = funnelToRequestCoroutine(cnt,
                            ((ExternalPointer)currentNode).getLinkId(),
                            new GetClosestPrecedingFingerRequest(findId),
                            GetClosestPrecedingFingerResponse.class);
                } catch (RuntimeException re) {
                    ctx.addOutgoingMessage(subAddress, logAddress,
                            warn("{} {} - Routing failed -- failed to get closest finger from {}",
                                    state.getSelfId(),
                                    subAddress,
                                    currentNode));
                    return null;
                }
                
                // special case -- if node we're querying returns itself for closest preceding finger, it means that its finger table its
                // empty (it is likely the first node making up the network). As such, if self, mark as found and return
                ExternalPointer newNode = state.toExternalPointer(gcpfr.getChordId(), gcpfr.getLinkId(),
                        ((ExternalPointer) currentNode).getLinkId());
                if (newNode.equals(currentNode)) {
                    foundPointer = newNode;
                    break;
                }

                currentNode = newNode;
            }
        }

        
        ctx.addOutgoingMessage(subAddress, logAddress,
                debug("{} {} - Routing to {} resulted in {}", state.getSelfId(), subAddress, findId, foundPointer));
        return foundPointer;
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, String destinationLinkId, Object message,
            Class<T> expectedResponseClass) throws Exception {
        Address destination = state.getAddressTransformer().linkIdToRemoteAddress(destinationLinkId);
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .sourceAddress(subAddress.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                .request(message)
                .timerAddress(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
