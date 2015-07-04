package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.error;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ChordClientCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Start start = ctx.getIncomingMessage();
        Address timerPrefix = start.getTimerPrefix();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        NodeId selfId = start.getNodeId();
        long seed = start.getSeed();
        Address bootstrapAddress = start.getBootstrapAddress();

        switchToStartedOnGraph(ctx, selfId, graphAddress);
        
        Set<Pointer> lastNotifiedPointers = new HashSet<>();
        try {
            State state = new State(seed, selfId);

            
            // Join (or just initialize if no bootstrap node is set)
            JoinSubcoroutine joinTask = new JoinSubcoroutine(Address.of("join"), state, timerPrefix, logAddress, bootstrapAddress);
            joinTask.run(cnt);

            switchToReadyOnGraph(ctx, selfId, graphAddress);
            lastNotifiedPointers = updateOutgoingLinksOnGraph(state, lastNotifiedPointers, ctx, selfId, graphAddress);

            
            // Create maintanence tasks that are supposed to run in parallel
            Address mainSourceId = Address.of("router");
            
            SubcoroutineRouter router = new SubcoroutineRouter(mainSourceId, ctx);
            Controller controller = router.getController();

            controller.add(
                    new UpdateOthersSubcoroutine(mainSourceId.appendSuffix("updateothers"), state, timerPrefix, logAddress),
                    AddBehaviour.ADD_PRIME);
            controller.add(
                    new FixFingerTableSubcoroutine(mainSourceId.appendSuffix("fixfinger"), state, timerPrefix, logAddress),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new StabilizeSubcoroutine(mainSourceId.appendSuffix("stabilize"), state, timerPrefix, logAddress),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new CheckPredecessorSubcoroutine(mainSourceId.appendSuffix("checkpred"), state, timerPrefix),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new IncomingRequestHandlerSubcoroutine(mainSourceId.appendSuffix("handler"), state, logAddress),
                    AddBehaviour.ADD);
            
            
            // Process messages
            while (true) {
                cnt.suspend();

                // if sent to main address then forward to incoming request handler, otherwise forward to router
                boolean forwardedToRouter = router.forward();
                if (!forwardedToRouter) {
                    Object msg = ctx.getIncomingMessage();
                    boolean isFromSelf = ctx.getSource().equals(ctx.getSelf());
                    if (isFromSelf && msg instanceof Kill) {
                        throw new RuntimeException("Kill message encountered");
                    }
                }
                
                lastNotifiedPointers = updateOutgoingLinksOnGraph(state, lastNotifiedPointers, ctx, selfId, graphAddress);
            }
        } catch (Exception e) {
            ctx.addOutgoingMessage(logAddress, error("Shutting down client {} -- {}", ctx.getSelf(), e));
        } finally {
            for (Pointer ptr : lastNotifiedPointers) {
                disconnectOnGraph(ctx, selfId, ptr.getId(), graphAddress);
            }
            switchToDeadOnGraph(ctx, selfId, graphAddress);
        }
    }

    private Set<Pointer> updateOutgoingLinksOnGraph(State state, Set<Pointer> lastNotifiedPointers, Context ctx, NodeId selfId, Address graphAddress) {
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

    private void switchToStartedOnGraph(Context ctx, NodeId selfId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: yellow"));
    }

    private void switchToReadyOnGraph(Context ctx, NodeId selfId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: green"));
    }

    private void switchToDeadOnGraph(Context ctx, NodeId selfId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: red"));
    }
    
    private void connectOnGraph(Context ctx, NodeId selfId, NodeId otherId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new AddEdge(selfId.toString(), otherId.toString()));
        ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfId.toString(), otherId.toString(),
                "-fx-stroke: rgba(0, 0, 0, .5); -fx-stroke-width: 3;"));
    }

    private void disconnectOnGraph(Context ctx, NodeId selfId, NodeId otherId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfId.toString(), otherId.toString()));
    }
}
