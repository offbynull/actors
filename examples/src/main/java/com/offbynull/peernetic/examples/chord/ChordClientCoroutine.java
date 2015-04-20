package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorRequest;
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
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import com.offbynull.peernetic.gateways.visualizer.AddEdge;
import com.offbynull.peernetic.gateways.visualizer.RemoveEdge;
import com.offbynull.peernetic.gateways.visualizer.StyleEdge;
import com.offbynull.peernetic.gateways.visualizer.StyleNode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        String graphAddress = start.getGraphAddress();
        NodeId selfId = start.getNodeId();
        String bootstrapAddress = start.getBootstrapAddress();

        switchToStartedOnGraph(ctx, selfId, graphAddress);
        
        Set<Pointer> lastNotifiedPointers = new HashSet<>();
        try {
            State state = new State(timerPrefix, selfId, bootstrapAddress);

            
            // Join (or just initialize if no bootstrap node is set)
            JoinTask joinTask = new JoinTask("join", state);
            joinTask.run(cnt);

            switchToReadyOnGraph(ctx, selfId, graphAddress);
            lastNotifiedPointers = updateOutgoingLinksOnGraph(state, lastNotifiedPointers, ctx, selfId, graphAddress);

            
            // Create parent coroutine and add maintenance tasks to it
            ParentCoroutine parentCoroutine = new ParentCoroutine("", ctx);
            
            parentCoroutine.add("updateothers", new UpdateOthersTask("updateothers", state)); // notify our fingers that we're here (finite)
            parentCoroutine.forceForward("updateothers", false);
            
            parentCoroutine.add("fixfinger", new FixFingerTableTask("fixfinger", state));
            parentCoroutine.forceForward("fixfinger", true);
            
            parentCoroutine.add("stabilize", new StabilizeTask("stabilize", state));
            parentCoroutine.forceForward("stabilize", true);
            
            parentCoroutine.add("checkpred", new CheckPredecessorTask("checkpred", state));
            parentCoroutine.forceForward("checkpred", true);
            

            while (true) {
                cnt.suspend();


                // Forward message to maintenance task. If the message wasn't for a maintenance task, try to handle it.
                boolean forwarded = parentCoroutine.forward();
                if (!forwarded) {
                    Object msg = ctx.getIncomingMessage();
                    String fromAddress = ctx.getSource();

                    LOG.debug("{} {} - Processing {} from {} to {}", state.getSelfId(), "", msg.getClass(), fromAddress,
                            ctx.getDestination());

                    if (msg instanceof GetIdRequest) {
                        GetIdRequest extMsg = (GetIdRequest) msg;
                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetIdResponse(extMsg.getId(), state.getSelfId()));
                    } else if (msg instanceof GetClosestPrecedingFingerRequest) {
                        GetClosestPrecedingFingerRequest extMsg = (GetClosestPrecedingFingerRequest) msg;

                        Pointer pointer = state.getClosestPrecedingFinger(extMsg.getChordId(), extMsg.getIgnoreIds());
                        NodeId id = pointer.getId();
                        String address = pointer instanceof ExternalPointer ? ((ExternalPointer) pointer).getAddress() : null;

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetClosestPrecedingFingerResponse(extMsg.getId(), id, address));
                    } else if (msg instanceof GetPredecessorRequest) {
                        GetPredecessorRequest extMsg = (GetPredecessorRequest) msg;

                        ExternalPointer pointer = state.getPredecessor();
                        NodeId id = pointer == null ? null : pointer.getId();
                        String address = pointer == null ? null : pointer.getAddress();

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetPredecessorResponse(extMsg.getId(), id, address));
                    } else if (msg instanceof GetSuccessorRequest) {
                        GetSuccessorRequest extMsg = (GetSuccessorRequest) msg;

                        List<Pointer> successors = state.getSuccessors();

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetSuccessorResponse(extMsg.getId(),successors));
                    } else if (msg instanceof NotifyRequest) {
                        NotifyRequest extMsg = (NotifyRequest) msg;

                        NodeId requesterId = extMsg.getChordId();
                        String requesterAddress = AddressUtils.removeSuffix(fromAddress, 2);

                        ExternalPointer newPredecessor = new ExternalPointer(requesterId, requesterAddress);
                        ExternalPointer existingPredecessor = state.getPredecessor();
                        if (existingPredecessor == null || requesterId.isWithin(existingPredecessor.getId(), true, state.getSelfId(), false)) {
                            state.setPredecessor(newPredecessor);
                        }

                        ExternalPointer pointer = state.getPredecessor();
                        NodeId id = pointer.getId();
                        String address = pointer.getAddress();

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new NotifyResponse(extMsg.getId(), id, address));
                    } else if (msg instanceof UpdateFingerTableRequest) {
                        UpdateFingerTableRequest extMsg = (UpdateFingerTableRequest) msg;
                        NodeId id = extMsg.getChordId();
                        String address = AddressUtils.removeSuffix(fromAddress, 2);
                        ExternalPointer newFinger = new ExternalPointer(id, address);

                        if (!state.isSelfId(id)) {
                            List<Pointer> oldFingers = state.getFingers();
                            boolean replaced = state.replaceFinger(newFinger);
                            List<Pointer> newFingers = state.getFingers();
                            LOG.debug("{} {} - Update finger with {}\nBefore: {}\nAfter: {}", state.getSelfId(), "",
                                    newFinger, oldFingers, newFingers);
//                            ExternalPointer pred = state.getPredecessor();
//                            if (replaced && pred != null) {
//                                ctx.addOutgoingMessage(
//                                        "ignore:ignore", // add 2 fake levels, because whoever gets this does a removeSuffix(2) (see above)
//                                        pred.getAddress(),
//                                        new UpdateFingerTableRequest(state.generateExternalMessageId(), id));
//                            }
                        }

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new UpdateFingerTableResponse(extMsg.getId()));
                    } else if (msg instanceof FindSuccessorRequest) {
                        FindSuccessorRequest extMsg = (FindSuccessorRequest) msg;
                        NodeId id = extMsg.getChordId();

                        String suffix = "remoteRouteTo" + state.generateExternalMessageId();
                        RemoteRouteToTask remoteRouteToTask = new RemoteRouteToTask(suffix, state, id, extMsg, ctx.getSource());
                        parentCoroutine.add(suffix, remoteRouteToTask);
                        parentCoroutine.forceForward(suffix, false);
                    } else if (msg instanceof Kill) {
                        return;
                    }
                }
                
                
                lastNotifiedPointers = updateOutgoingLinksOnGraph(state, lastNotifiedPointers, ctx, selfId, graphAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            for (Pointer ptr : lastNotifiedPointers) {
                disconnectOnGraph(ctx, selfId, ptr.getId(), graphAddress);
            }
            switchToDeadOnGraph(ctx, selfId, graphAddress);
        }
    }

    private Set<Pointer> updateOutgoingLinksOnGraph(State state, Set<Pointer> lastNotifiedPointers, Context ctx, NodeId selfId, String graphAddress) {
        // Send link changes to graph
        Set<Pointer> newPointers = new HashSet<>(Arrays.<Pointer>asList(
                state.getFingers().stream().filter(x -> x instanceof ExternalPointer).toArray(x -> new Pointer[x])));
        if (state.getPredecessor() != null) {
            newPointers.add(state.getPredecessor());
        }
        
        Set<Pointer> addedPointers = new HashSet<>(newPointers);
        addedPointers.removeAll(lastNotifiedPointers);
        addedPointers.forEach(x -> connectOnGraph(ctx, selfId, x.getId(), graphAddress));
        
        Set<Pointer> removedPointers = new HashSet<>(lastNotifiedPointers);
        removedPointers.removeAll(newPointers);
        removedPointers.forEach(x -> disconnectOnGraph(ctx, selfId, x.getId(), graphAddress));
        
        lastNotifiedPointers = newPointers;
        return lastNotifiedPointers;
    }

    private void switchToStartedOnGraph(Context ctx, NodeId selfId, String graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: yellow"));
    }

    private void switchToReadyOnGraph(Context ctx, NodeId selfId, String graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: green"));
    }

    private void switchToDeadOnGraph(Context ctx, NodeId selfId, String graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: red"));
    }
    
    private void connectOnGraph(Context ctx, NodeId selfId, NodeId otherId, String graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new AddEdge(selfId.toString(), otherId.toString()));
        ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfId.toString(), otherId.toString(),
                "-fx-stroke: rgba(0, 0, 0, .5); -fx-stroke-width: 3;"));
    }

    private void disconnectOnGraph(Context ctx, NodeId selfId, NodeId otherId, String graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfId.toString(), otherId.toString()));
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
