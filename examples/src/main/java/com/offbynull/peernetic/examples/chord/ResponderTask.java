package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerResponse;
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
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResponderTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(ResponderTask.class);

    private final String sourceId;
    private final State state;

    public ResponderTask(String sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        while (true) {
            cnt.suspend();
            
            Object msg = ctx.getIncomingMessage();
            String fromAddress = ctx.getSource();
            
            

            LOG.debug("Incoming request {}", msg.getClass());
            
            if (msg instanceof GetIdRequest) {
                GetIdRequest extMsg = (GetIdRequest) msg;
                addOutgoingExternalMessage(ctx,
                        fromAddress,
                        new GetIdResponse(extMsg.getId(), state.getSelfId().getValueAsByteArray()));
            } else if (msg instanceof GetClosestFingerRequest) {
                GetClosestFingerRequest extMsg = (GetClosestFingerRequest) msg;
                
                Pointer pointer = state.getClosestFinger(extMsg);
                NodeId id = pointer.getId();
                String address = pointer instanceof ExternalPointer ? ((ExternalPointer) pointer).getAddress() : null;
                
                addOutgoingExternalMessage(ctx,
                        fromAddress,
                        new GetClosestFingerResponse(extMsg.getId(), id.getValueAsByteArray(), address));
            } else if (msg instanceof GetClosestPrecedingFingerRequest) {
                GetClosestPrecedingFingerRequest extMsg = (GetClosestPrecedingFingerRequest) msg;
                
                Pointer pointer = state.getClosestPrecedingFinger(extMsg);
                NodeId id = pointer.getId();
                String address = pointer instanceof ExternalPointer ? ((ExternalPointer) pointer).getAddress() : null;

                addOutgoingExternalMessage(ctx,
                        fromAddress,
                        new GetClosestPrecedingFingerResponse(extMsg.getId(), id.getValueAsByteArray(), address));
            } else if (msg instanceof GetPredecessorRequest) {
                GetPredecessorRequest extMsg = (GetPredecessorRequest) msg;

                ExternalPointer pointer = state.getPredecessor();
                NodeId id = pointer.getId();
                String address = pointer.getAddress();
                
                addOutgoingExternalMessage(ctx,
                        fromAddress,
                        new GetPredecessorResponse(extMsg.getId(), id.getValueAsByteArray(), address));
            } else if (msg instanceof GetSuccessorRequest) {
                GetSuccessorRequest extMsg = (GetSuccessorRequest) msg;
                
                List<Pointer> successors = state.getSuccessors();
                
                addOutgoingExternalMessage(ctx,
                        fromAddress,
                        new GetSuccessorResponse(extMsg.getId(),successors));
            } else if (msg instanceof NotifyRequest) {
                NotifyRequest extMsg = (NotifyRequest) msg;

                NodeId requesterId = state.toId(extMsg.getChordId());

                ExternalPointer newPredecessor = new ExternalPointer(requesterId, fromAddress);
                ExternalPointer existingPredecessor = state.getPredecessor();
                if (existingPredecessor == null || requesterId.isWithin(existingPredecessor.getId(), true, state.getSelfId(), false)) {
                    state.setPredecessor(newPredecessor);
                }

                ExternalPointer pointer = state.getPredecessor();
                NodeId id = pointer.getId();
                String address = pointer.getAddress();
                
                addOutgoingExternalMessage(ctx,
                        fromAddress,
                        new NotifyResponse(extMsg.getId(), id.getValueAsByteArray(), address));
            } else if (msg instanceof UpdateFingerTableRequest) {
                UpdateFingerTableRequest extMsg = (UpdateFingerTableRequest) msg;
                NodeId id = state.toId(extMsg.getChordId());
                ExternalPointer newFinger = new ExternalPointer(id, fromAddress);

                if (!state.isSelfId(id)) {
                    boolean replaced = state.replaceFinger(newFinger);
                    ExternalPointer pred = state.getPredecessor();
                    if (replaced && pred != null) {
                        addOutgoingExternalMessage(ctx,
                                pred.getAddress(),
                                new UpdateFingerTableRequest(state.generateExternalMessageId(), id.getValueAsByteArray()));
                    }
                }

                addOutgoingExternalMessage(ctx,
                        fromAddress,
                        new UpdateFingerTableResponse(state.generateExternalMessageId()));
            } else if (msg instanceof FindSuccessorRequest) {
                FindSuccessorRequest extMsg = (FindSuccessorRequest) msg;
                NodeId id = state.toId(extMsg.getChordId());

                try {
                    // we don't want to block the responder task by waiting for remoterouteto to complete
                    // response sent from within task
                    taskHelper.fireRemoteRouteToTask(id, extMsg, getSource());
                } catch (Exception e) {
                    // should never happen
                }
            }
        }
    }
    
    private void addOutgoingExternalMessage(Context ctx, String destination, ExternalMessage message) {
        Validate.notNull(ctx);
        Validate.notNull(destination);
        Validate.notNull(message);
        
        ctx.addOutgoingMessage(
                AddressUtils.parentize(sourceId, "" + message.getId()),
                destination,
                message);
    }
}
