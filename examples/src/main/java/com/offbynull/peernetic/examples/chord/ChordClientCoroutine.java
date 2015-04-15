package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
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
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChordClientCoroutine implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChordClientCoroutine.class);

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Start start = ctx.getIncomingMessage();
        String timerPrefix = start.getTimerPrefix();
        
        State state = new State(timerPrefix);
        ParentCoroutine parentCoroutine = new ParentCoroutine("", ctx);
        
        // JOIN (or just initialize if no bootstrap node is set)
        parentCoroutine.add("join", new JoinTask("join", state));
        parentCoroutine.runUntilFinished(cnt); // run until all added coroutines are finished and removed
        
        // RUN
        parentCoroutine.add("updateothers", new UpdateOthersTask("updateothers", state)); // notify our fingers that we're here (finite)
        parentCoroutine.add("fixfinger", new FixFingerTableTask("fixfinger", state));
        parentCoroutine.add("stabilize", new StabilizeTask("fixfinger", state));
        parentCoroutine.add("checkpred", new CheckPredecessorTask("checkpred", state));
        
        while (true) {
            cnt.suspend();
            
            
            boolean forwarded = parentCoroutine.forward(); // run until all added coroutines are finished and removed
            if (forwarded) {
                continue;
            }
            
            
            
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
                
                String suffix = "routeTo" + state.generateExternalMessageId();
                parentCoroutine.add(suffix, new RouteToTask(suffix, state, id));
            }
        }
    }
    
    private void addOutgoingExternalMessage(Context ctx, String destination, ExternalMessage message) {
        Validate.notNull(ctx);
        Validate.notNull(destination);
        Validate.notNull(message);
        
        ctx.addOutgoingMessage(
                "" + message.getId(),
                destination,
                message);
    }
    
}
