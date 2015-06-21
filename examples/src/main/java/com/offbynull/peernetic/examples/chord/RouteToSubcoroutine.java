package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import org.apache.commons.lang3.Validate;

final class RouteToSubcoroutine implements Subcoroutine<Pointer> {

    private final Address sourceId;
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    
    private final NodeId findId;

    public RouteToSubcoroutine(Address sourceId, State state, Address timerAddress, Address logAddress, NodeId findId) {
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
        
        NodeId selfId = state.getSelfId();
        
        ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Routing to {}", state.getSelfId(), sourceId, findId));
        
        
        Pointer foundPointer;
        Pointer currentNode = state.getSelfPointer();
        while (true) {
            ctx.addOutgoingMessage(sourceId, logAddress,
                    debug("{} {} - Search for {} moving forward to {}", state.getSelfId(), sourceId, findId, currentNode));
            
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
                                new GetSuccessorRequest(),
                                GetSuccessorResponse.class);
                    successorId = gsr.getEntries().get(0).getChordId();
                } catch (RuntimeException re) {
                    ctx.addOutgoingMessage(sourceId, logAddress,
                            warn("{} {} - Routing failed -- failed to get successor from {}", state.getSelfId(), sourceId, currentNode));
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
                            ((ExternalPointer)currentNode).getAddress(),
                            new GetClosestPrecedingFingerRequest(findId),
                            GetClosestPrecedingFingerResponse.class);
                } catch (RuntimeException re) {
                    ctx.addOutgoingMessage(sourceId, logAddress,
                            warn("{} {} - Routing failed -- failed to get closest finger from {}",
                                    state.getSelfId(),
                                    sourceId,
                                    currentNode));
                    return null;
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

        
        ctx.addOutgoingMessage(sourceId, logAddress,
                debug("{} {} - Routing to {} resulted in {}", state.getSelfId(), sourceId, findId, foundPointer));
        return foundPointer;
    }
    
    @Override
    public Address getId() {
        return sourceId;
    }
    
    private <T> T funnelToRequestCoroutine(Continuation cnt, Address destination, Object message,
            Class<T> expectedResponseClass) throws Exception {
        RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                .id(sourceId.appendSuffix(state.nextRandomId()))
                .destinationAddress(destination)
                .request(message)
                .timerAddressPrefix(timerAddress)
                .addExpectedResponseType(expectedResponseClass)
                .build();
        return requestSubcoroutine.run(cnt);
    }
}
