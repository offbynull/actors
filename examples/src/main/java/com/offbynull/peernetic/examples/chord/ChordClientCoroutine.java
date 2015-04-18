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
import com.offbynull.peernetic.gateways.visualizer.AddNode;
import com.offbynull.peernetic.gateways.visualizer.MoveNode;
import com.offbynull.peernetic.gateways.visualizer.PositionUtils;
import com.offbynull.peernetic.gateways.visualizer.StyleNode;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        String graphAddress = start.getGraphAddress();
        NodeId selfId = start.getNodeId();
        String bootstrapAddress = start.getBootstrapAddress();

        try {
            addToGraph(ctx, selfId, graphAddress);

            State state = new State(timerPrefix, selfId, bootstrapAddress);
            ParentCoroutine parentCoroutine = new ParentCoroutine("", ctx);

            // JOIN (or just initialize if no bootstrap node is set)
            JoinTask joinTask = new JoinTask("join", state);
            joinTask.run(cnt);

            switchToReadyOnGraph(ctx, selfId, graphAddress);

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
                            new GetIdResponse(extMsg.getId(), state.getSelfId()));
                } else if (msg instanceof GetClosestFingerRequest) {
                    GetClosestFingerRequest extMsg = (GetClosestFingerRequest) msg;

                    Pointer pointer = state.getClosestFinger(extMsg);
                    NodeId id = pointer.getId();
                    String address = pointer instanceof ExternalPointer ? ((ExternalPointer) pointer).getAddress() : null;

                    addOutgoingExternalMessage(ctx,
                            fromAddress,
                            new GetClosestFingerResponse(extMsg.getId(), id, address));
                } else if (msg instanceof GetClosestPrecedingFingerRequest) {
                    GetClosestPrecedingFingerRequest extMsg = (GetClosestPrecedingFingerRequest) msg;

                    Pointer pointer = state.getClosestPrecedingFinger(extMsg);
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
                            new NotifyResponse(extMsg.getId(), id, address));
                } else if (msg instanceof UpdateFingerTableRequest) {
                    UpdateFingerTableRequest extMsg = (UpdateFingerTableRequest) msg;
                    NodeId id = extMsg.getChordId();
                    ExternalPointer newFinger = new ExternalPointer(id, fromAddress);

                    if (!state.isSelfId(id)) {
                        boolean replaced = state.replaceFinger(newFinger);
                        ExternalPointer pred = state.getPredecessor();
                        if (replaced && pred != null) {
                            addOutgoingExternalMessage(ctx,
                                    pred.getAddress(),
                                    new UpdateFingerTableRequest(state.generateExternalMessageId(), id));
                        }
                    }

                    addOutgoingExternalMessage(ctx,
                            fromAddress,
                            new UpdateFingerTableResponse(state.generateExternalMessageId()));
                } else if (msg instanceof FindSuccessorRequest) {
                    FindSuccessorRequest extMsg = (FindSuccessorRequest) msg;
                    NodeId id = extMsg.getChordId();

                    String suffix = "remoteRouteTo" + state.generateExternalMessageId();
                    RemoteRouteToTask remoteRouteToTask = new RemoteRouteToTask(suffix, state, id, extMsg, ctx.getSource());
                    parentCoroutine.add(suffix, remoteRouteToTask);
                    parentCoroutine.forceForward(suffix, false);
                }
            }
        } catch (Exception e) {
            switchToErrorOnGraph(ctx, selfId, graphAddress);
            e.printStackTrace();
        }
    }

    private void addToGraph(Context ctx, NodeId selfId, String graphAddress) {
        BigDecimal idDec = new BigDecimal(selfId.getValueAsBigInteger());
        BigDecimal limitDec = new BigDecimal(selfId.getLimitAsBigInteger()).add(BigDecimal.ONE);
        double percentage = idDec.divide(limitDec, 10, RoundingMode.FLOOR).doubleValue();
        Point newPoint = PositionUtils.pointOnCircle(200, percentage);
        ctx.addOutgoingMessage(graphAddress, new AddNode(selfId.toString()));
        ctx.addOutgoingMessage(graphAddress, new MoveNode(selfId.toString(), newPoint.getX(), newPoint.getY()));
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: yellow"));
    }

    private void switchToReadyOnGraph(Context ctx, NodeId selfId, String graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: green"));
    }

    private void switchToErrorOnGraph(Context ctx, NodeId selfId, String graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: red"));
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
