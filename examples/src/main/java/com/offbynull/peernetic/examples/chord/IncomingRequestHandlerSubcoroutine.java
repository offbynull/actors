package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class IncomingRequestHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    
    private final State state;

    private final Address logAddress;

    public IncomingRequestHandlerSubcoroutine(Address subAddress, State state, Address logAddress) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(logAddress);
        this.subAddress = subAddress;
        this.state = state;
        this.logAddress = logAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Address self = ctx.getSelf();
        
        while (true) {
            cnt.suspend();
            
            Object msg = ctx.getIncomingMessage();
            Address fromAddress = ctx.getSource();
            Address toAddress = ctx.getDestination();

            ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Processing {} from {} to {}", state.getSelfId(), subAddress,
                    msg.getClass(), fromAddress, toAddress));

            if (msg instanceof GetIdRequest) {
                ctx.addOutgoingMessage(subAddress, fromAddress, new GetIdResponse(state.getSelfId()));
            } else if (msg instanceof GetClosestPrecedingFingerRequest) {
                GetClosestPrecedingFingerRequest extMsg = (GetClosestPrecedingFingerRequest) msg;

                Pointer pointer = state.getClosestPrecedingFinger(extMsg.getChordId(), extMsg.getIgnoreIds());
                NodeId id = pointer.getId();
                String linkId = pointer instanceof ExternalPointer ? ((ExternalPointer) pointer).getLinkId() : null;

                ctx.addOutgoingMessage(subAddress, fromAddress, new GetClosestPrecedingFingerResponse(id, linkId));
            } else if (msg instanceof GetPredecessorRequest) {
                ExternalPointer pointer = state.getPredecessor();
                NodeId id = pointer == null ? null : pointer.getId();
                String linkId = pointer == null ? null : pointer.getLinkId();

                ctx.addOutgoingMessage(subAddress, fromAddress, new GetPredecessorResponse(id, linkId));
            } else if (msg instanceof GetSuccessorRequest) {
                List<Pointer> successors = state.getSuccessors();

                ctx.addOutgoingMessage(subAddress, fromAddress, new GetSuccessorResponse(successors));
            } else if (msg instanceof NotifyRequest) {
                NotifyRequest extMsg = (NotifyRequest) msg;

                NodeId requesterId = extMsg.getChordId();
                Address requesterAddress = fromAddress.removeSuffix(3); // e.g. actor:0:router:stabilize:2414144054146105661 -> actor:0
                String requesterLinkId = state.getAddressTransformer().remoteAddressToLinkId(requesterAddress);

                ExternalPointer newPredecessor = new ExternalPointer(requesterId, requesterLinkId);
                ExternalPointer existingPredecessor = state.getPredecessor();
                if (existingPredecessor == null || requesterId.isWithin(existingPredecessor.getId(), true, state.getSelfId(), false)) {
                    state.setPredecessor(newPredecessor);
                }

                ExternalPointer pointer = state.getPredecessor();
                NodeId id = pointer.getId();
                String linkId = pointer.getLinkId();

                ctx.addOutgoingMessage(subAddress, fromAddress, new NotifyResponse(id, linkId));
            } else if (msg instanceof UpdateFingerTableRequest) {
                UpdateFingerTableRequest extMsg = (UpdateFingerTableRequest) msg;
                NodeId id = extMsg.getChordId();
                Address address = fromAddress.removeSuffix(3); // e.g. actor:2:router:updateothers:4562701804747358225 -> actor:2
                String linkId = state.getAddressTransformer().remoteAddressToLinkId(address);
                ExternalPointer newFinger = new ExternalPointer(id, linkId);

                if (!state.isSelfId(id)) {
                    List<Pointer> oldFingers = state.getFingers();
                    boolean replaced = state.replaceFinger(newFinger);
                    List<Pointer> newFingers = state.getFingers();
                    ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Update finger with {}\nBefore: {}\nAfter: {}",
                            state.getSelfId(), subAddress, newFinger, oldFingers, newFingers));
                }

                ctx.addOutgoingMessage(subAddress, fromAddress, new UpdateFingerTableResponse());
            } else {
                ctx.addOutgoingMessage(subAddress, logAddress, warn("{} {} - Unrecognized message from {}: {}", state.getSelfId(), subAddress,
                        msg.getClass(), fromAddress, toAddress));
            }
        }
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }
}