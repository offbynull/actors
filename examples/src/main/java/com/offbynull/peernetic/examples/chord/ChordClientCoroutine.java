package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.error;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.chord.AddressConstants.JOIN_RELATIVE_ADDRESS;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_CHECKPRED_RELATIVE_ADDRESS;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_FIXFINGER_RELATIVE_ADDRESS;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_RELATIVE_ADDRESS;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_STABILIZE_RELATIVE_ADDRESS;
import static com.offbynull.peernetic.examples.chord.AddressConstants.ROUTER_UPDATEOTHERS_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

public final class ChordClientCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Start start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerAddress();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        NodeId selfId = start.getNodeId();
        long seed = start.getSeed();
        String bootstrapLinkId = start.getBootstrapLinkId();
        AddressTransformer addressTransformer = start.getAddressTransformer();

        switchToStartedOnGraph(ctx, selfId, graphAddress);

        Set<GraphLink> lastOutgoingLinks = new HashSet<>();
        try {
            State state = new State(timerAddress, graphAddress, logAddress, seed, selfId, addressTransformer);

            // Join (or just initialize if no bootstrap node is set)
            JoinSubcoroutine joinTask = new JoinSubcoroutine(JOIN_RELATIVE_ADDRESS, state, bootstrapLinkId);
            joinTask.run(cnt);

            switchToReadyOnGraph(ctx, selfId, graphAddress);
            lastOutgoingLinks = updateOutgoingLinksOnGraph(state, lastOutgoingLinks, ctx, selfId, graphAddress);

            // Create maintanence tasks that are supposed to run in parallel
            SubcoroutineRouter router = new SubcoroutineRouter(ROUTER_RELATIVE_ADDRESS, ctx);
            Controller controller = router.getController();

            controller.add(
                    new UpdateOthersSubcoroutine(ROUTER_UPDATEOTHERS_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD_PRIME);
            controller.add(
                    new FixFingerTableSubcoroutine(ROUTER_FIXFINGER_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new StabilizeSubcoroutine(ROUTER_STABILIZE_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new CheckPredecessorSubcoroutine(ROUTER_CHECKPRED_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new IncomingRequestHandlerSubcoroutine(ROUTER_HANDLER_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD);

            // Process messages
            while (true) {
                cnt.suspend();

                // if sent to main address then forward to incoming request handler, otherwise forward to router
                boolean forwardedToRouter = router.forward().isForwarded();
                if (!forwardedToRouter) {
                    Object msg = ctx.getIncomingMessage();
                    boolean isFromSelf = ctx.getSource().equals(ctx.getSelf());
                    if (isFromSelf && msg instanceof Kill) {
                        throw new RuntimeException("Kill message encountered");
                    }
                }

                lastOutgoingLinks = updateOutgoingLinksOnGraph(state, lastOutgoingLinks, ctx, selfId, graphAddress);
            }
        } catch (Exception e) {
            ctx.addOutgoingMessage(logAddress, error("Shutting down client {} -- {}", ctx.getSelf(), e));
        } finally {
            for (GraphLink link : lastOutgoingLinks) {
                disconnectOnGraph(ctx, selfId, link.getExternalPointer().getId(), graphAddress);
            }
            switchToDeadOnGraph(ctx, selfId, graphAddress);
        }
    }

    private Set<GraphLink> updateOutgoingLinksOnGraph(State state, Set<GraphLink> lastOutgoingLinks, Context ctx, NodeId selfId,
            Address graphAddress) {
        // Send link changes to graph
        Set<GraphLink> newLinks = new HashSet<>();

        Set<ExternalPointer> fingers = state.getFingers().stream()
                .filter(x -> x instanceof ExternalPointer)
                .map(x -> (ExternalPointer) x)
                .collect(Collectors.toSet());
        // for visualization purposes, only use the immediate successor -- the visualization makes more sense this way because the paper
        // for the most part only talks about having 1 successor when its describing the algorithm (a successor table is described later on)
        Set<ExternalPointer> successors = state.getSuccessors().subList(0, 1).stream()
                .filter(x -> x instanceof ExternalPointer)
                .map(x -> (ExternalPointer) x)
                .collect(Collectors.toSet());
        ExternalPointer predecessor = state.getPredecessor();

        // Line colors change depending on what kind of link it is
        // 
        // If it's a finger, the red component will be set
        // If it's a successor, the green component will be set
        // If it's a predecssor, the blue component will be set
        //
        // Finger = red
        // Successor = green
        // Predecessor = blue
        // Finger + Successor =  red + green = yellow
        // Finger + Predecssor = red + blue = purple
        // Finger + Successor + Predecssor = red + green + blue = white
        Consumer<ExternalPointer> addToNewLinksConsumer = x -> {
            String color = "rgb("
                        + (fingers.contains(x) ? "255" : "0") + ", "
                        + (successors.contains(x) ? "255" : "0") + ", "
                        + (x.equals(predecessor) ? "255" : "0")
                        + ");";
            newLinks.add(new GraphLink(x, color));
        };
        
        fingers.stream().forEach(addToNewLinksConsumer);
        successors.stream().forEach(addToNewLinksConsumer);
        if (predecessor != null) {
            addToNewLinksConsumer.accept(predecessor);
        }

        Set<GraphLink> removedPointers = new HashSet<>(lastOutgoingLinks);
        removedPointers.removeAll(newLinks);
        removedPointers.forEach(x -> disconnectOnGraph(ctx, selfId, x.getExternalPointer().getId(), graphAddress));

        Set<GraphLink> addedPointers = new HashSet<>(newLinks);
        addedPointers.removeAll(lastOutgoingLinks);
        addedPointers.forEach(x -> connectOnGraph(ctx, selfId, x.getExternalPointer().getId(), x.getColor(), graphAddress));

        lastOutgoingLinks = newLinks;
        return lastOutgoingLinks;
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

    private void connectOnGraph(Context ctx, NodeId selfId, NodeId otherId, String color,
            Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new AddEdge(selfId.toString(), otherId.toString()));
        ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfId.toString(), otherId.toString(),
                " -fx-stroke-width: 3; -fx-stroke: " + color));
    }

    private void disconnectOnGraph(Context ctx, NodeId selfId, NodeId otherId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfId.toString(), otherId.toString()));
    }

    private static final class GraphLink {

        private final ExternalPointer externalPointer;
        private final String color;

        public GraphLink(ExternalPointer externalPointer, String color) {
            Validate.notNull(externalPointer);
            Validate.notNull(color);
            this.externalPointer = externalPointer;
            this.color = color;
        }

        public ExternalPointer getExternalPointer() {
            return externalPointer;
        }

        public String getColor() {
            return color;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + Objects.hashCode(this.externalPointer);
            hash = 37 * hash + Objects.hashCode(this.color);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GraphLink other = (GraphLink) obj;
            if (!Objects.equals(this.externalPointer, other.externalPointer)) {
                return false;
            }
            if (!Objects.equals(this.color, other.color)) {
                return false;
            }
            return true;
        }

    }
}
