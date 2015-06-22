package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
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
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class IncomingMessageHandlerCoroutine implements Coroutine {

    private final State state;

    private final Address logAddress;

    public IncomingMessageHandlerCoroutine(State state, Address logAddress) {
        Validate.notNull(state);
        Validate.notNull(logAddress);
        this.state = state;
        this.logAddress = logAddress;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        while (true) {
            Object msg = ctx.getIncomingMessage();
            Address fromAddress = ctx.getSource();
            Address toAddress = ctx.getDestination();

            ctx.addOutgoingMessage(logAddress, debug("{} {} - Processing {} from {} to {}", state.getSelfId(), "", msg.getClass(),
                    fromAddress, toAddress));

            if (msg instanceof GetIdRequest) {
                ctx.addOutgoingMessage(toAddress, fromAddress, new GetIdResponse(state.getSelfId()));
            } else if (msg instanceof GetClosestPrecedingFingerRequest) {
                GetClosestPrecedingFingerRequest extMsg = (GetClosestPrecedingFingerRequest) msg;

                Pointer pointer = state.getClosestPrecedingFinger(extMsg.getChordId(), extMsg.getIgnoreIds());
                NodeId id = pointer.getId();
                Address address = pointer instanceof ExternalPointer ? ((ExternalPointer) pointer).getAddress() : null;

                ctx.addOutgoingMessage(toAddress, fromAddress, new GetClosestPrecedingFingerResponse(id, address));
            } else if (msg instanceof GetPredecessorRequest) {
                ExternalPointer pointer = state.getPredecessor();
                NodeId id = pointer == null ? null : pointer.getId();
                Address address = pointer == null ? null : pointer.getAddress();

                ctx.addOutgoingMessage(toAddress, fromAddress, new GetPredecessorResponse(id, address));
            } else if (msg instanceof GetSuccessorRequest) {
                List<Pointer> successors = state.getSuccessors();

                ctx.addOutgoingMessage(toAddress, fromAddress, new GetSuccessorResponse(successors));
            } else if (msg instanceof NotifyRequest) {
                NotifyRequest extMsg = (NotifyRequest) msg;

                NodeId requesterId = extMsg.getChordId();
                Address requesterAddress = fromAddress.removeSuffix(2);

                ExternalPointer newPredecessor = new ExternalPointer(requesterId, requesterAddress);
                ExternalPointer existingPredecessor = state.getPredecessor();
                if (existingPredecessor == null || requesterId.isWithin(existingPredecessor.getId(), true, state.getSelfId(), false)) {
                    state.setPredecessor(newPredecessor);
                }

                ExternalPointer pointer = state.getPredecessor();
                NodeId id = pointer.getId();
                Address address = pointer.getAddress();

                ctx.addOutgoingMessage(toAddress, fromAddress, new NotifyResponse(id, address));
            } else if (msg instanceof UpdateFingerTableRequest) {
                UpdateFingerTableRequest extMsg = (UpdateFingerTableRequest) msg;
                NodeId id = extMsg.getChordId();
                Address address = fromAddress.removeSuffix(2);
                ExternalPointer newFinger = new ExternalPointer(id, address);

                if (!state.isSelfId(id)) {
                    List<Pointer> oldFingers = state.getFingers();
                    boolean replaced = state.replaceFinger(newFinger);
                    List<Pointer> newFingers = state.getFingers();
                    ctx.addOutgoingMessage(logAddress, debug("{} {} - Update finger with {}\nBefore: {}\nAfter: {}",
                            state.getSelfId(), "", newFinger, oldFingers, newFingers));
                }

                ctx.addOutgoingMessage(toAddress, fromAddress, new UpdateFingerTableResponse());
            } else if (msg instanceof Kill) {
                throw new RuntimeException("Kill command arrived");
            }
            
            cnt.suspend();
        }
    }
}