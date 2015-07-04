package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import org.apache.commons.lang3.Validate;

// unique to initialization phase in that it doesn't consider you as a node in the network (you're initializing, you haven't connected yet)
final class InitRouteToCoroutine implements Coroutine {

    private final Address sourceId;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    private final ExternalPointer bootstrapNode;
    private final NodeId findId;

    private ExternalPointer foundPointer;

    public InitRouteToCoroutine(Address sourceId, State state, Address timerAddress, Address logAddress, ExternalPointer bootstrapNode,
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
        this.findId = findId;
        this.bootstrapNode = bootstrapNode;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Routing to {}", state.getSelfId(), sourceId, findId));
        
        ExternalPointer currentNode = bootstrapNode;
        while (true) {
            NodeId successorId;
            GetSuccessorResponse gsr;
            try {
                gsr = funnelToRequestCoroutine(
                            cnt,
                            currentNode.getAddress(),
                            new GetSuccessorRequest(),
                            GetSuccessorResponse.class);
                successorId = gsr.getEntries().get(0).getChordId();
            } catch (RuntimeException re) {
                ctx.addOutgoingMessage(sourceId, logAddress,
                        warn("{} {} - Routing failed -- failed to get successor from {}", state.getSelfId(), sourceId, currentNode));
                return;
            }

            if (findId.isWithin(currentNode.getId(), false, successorId, true) ||
                    successorId.equals(currentNode.getId())) {
                foundPointer = currentNode;
                break;
            }

            GetClosestPrecedingFingerResponse gcpfr;
            try {
                gcpfr = funnelToRequestCoroutine(cnt,
                        currentNode.getAddress(),
                        new GetClosestPrecedingFingerRequest(findId),
                        GetClosestPrecedingFingerResponse.class);
            } catch (RuntimeException re) {
                ctx.addOutgoingMessage(sourceId, logAddress,
                        warn("{} {} - Routing failed -- failed to get closest finger from {}", state.getSelfId(), sourceId, currentNode));
                return;
            }

                // special case -- if node we're querying returns itself for closest preceding finger, it means that its finger table its
            // empty (it is likely the first node making up the network). As such, if self, mark as found and return
            ExternalPointer newNode = state.toExternalPointer(gcpfr.getChordId(), gcpfr.getAddress(), currentNode.getAddress());
            if (newNode.equals(currentNode)) {
                foundPointer = newNode;
                break;
            }
            
            currentNode = newNode;
        }

        ctx.addOutgoingMessage(sourceId, logAddress,
                debug("{} {} - Routing to {} resulting in {} at {}", state.getSelfId(), sourceId, findId, foundPointer));
    }

    public ExternalPointer getResult() {
        return foundPointer;
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
