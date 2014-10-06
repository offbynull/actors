package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.GatewayEndpointDirectory;
import com.offbynull.peernetic.GatewayEndpointIdentifier;
import com.offbynull.peernetic.GatewayInputAdapter;
import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.SimpleEndpointScheduler;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.debug.localgateway.LocalGateway;
import com.offbynull.peernetic.debug.localgateway.LocalGatewayHub;
import com.offbynull.peernetic.debug.actornetwork.SimpleLine;
import com.offbynull.peernetic.debug.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.debug.visualizer.AddNodeCommand;
import com.offbynull.peernetic.debug.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.debug.visualizer.JGraphXVisualizer;
import com.offbynull.peernetic.debug.visualizer.RemoveEdgeCommand;
import com.offbynull.peernetic.debug.visualizer.Visualizer;
import com.offbynull.peernetic.debug.visualizer.VisualizerUtils;
import com.offbynull.peernetic.network.Gateway;
import com.offbynull.peernetic.network.GatewayListener;
import com.offbynull.peernetic.network.XStreamSerializer;
import com.offbynull.peernetic.playground.chorddht.ChordActiveListener.Mode;
import com.offbynull.peernetic.playground.chorddht.messages.internal.Start;
import java.awt.Color;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Main {

    public static void main(String[] args) throws Throwable {
        Actor[] actors = new Actor[32];

        // Start visualizer
        Visualizer<Id> visualizer = new JGraphXVisualizer<>();
        visualizer.visualize();

        ChordActiveListener<Id> activeListener = (id, mode) -> {
            visualizer.step("Activating node " + id + " as " + mode,
                    new ChangeNodeCommand<>(id, null, null, Color.GREEN));
        };

        ChordLinkListener<Id> linkListener = (id, dstId) -> {
            String msg = "Connected " + id + " to " + dstId;
            visualizer.step(msg, new AddEdgeCommand<>(id, dstId));
        };

        ChordUnlinkListener<Id> unlinkListener = (id, dstId) -> {
            String msg = "Disconnected " + id + " from " + dstId;
            visualizer.step(msg, new RemoveEdgeCommand<>(id, dstId));
        };

        // Create actors
        for (int i = 0; i < actors.length; i++) {
            ChordClient<? extends Object> chordClient = new ChordClient<>();
            actors[i] = new JavaflowActor(chordClient);
        }

        // Start actors all within the same thread
        ActorRunnable actorRunnable = ActorRunnable.createAndStart(actors);

        EndpointScheduler endpointScheduler = new SimpleEndpointScheduler();
        LocalGatewayHub<Integer> gatewayHub = new LocalGatewayHub<>(
                new SimpleLine<>(),//0L, Duration.ofMillis(500L), Duration.ofMillis(100L), 0.0, 0.9, 10),
                new XStreamSerializer());
        for (int i = 0; i < actors.length; i++) {
            Endpoint endpoint = actorRunnable.getEndpoint(actors[i]);

            int address = i;
            GatewayListener<Integer> gatewayListener = new GatewayInputAdapter<>(endpoint);
            LocalGateway<Integer> gateway = new LocalGateway<>(address, gatewayHub, gatewayListener);

            Id id = generateId(i, actors.length);
            if (i == 0) {
                linkToGatewayAndSeedStart(activeListener, linkListener, unlinkListener, i, actors.length, endpoint, gateway, endpointScheduler);
                showNode(visualizer, id, Mode.SEED);
            } else {
                linkToGatewayAndBootstrapStart(activeListener, linkListener, unlinkListener, i, actors.length, 0, endpoint, gateway, endpointScheduler);
                showNode(visualizer, id, Mode.JOIN);
            }
        }
    }

    private static <T> void linkToGatewayAndSeedStart(
            ChordActiveListener<Id> activeListener, ChordLinkListener<Id> linkListener, ChordUnlinkListener<Id> unlinkListener,
            int selfId, int idLen, Endpoint selfEndpoint, Gateway<T> gateway, EndpointScheduler endpointScheduler) {
        EndpointDirectory<T> endpointDirectory = new GatewayEndpointDirectory<>(gateway);
        EndpointIdentifier<T> endpointIdentifier = new GatewayEndpointIdentifier<>();

        Start<T> start = new Start<>(activeListener, linkListener, unlinkListener, endpointDirectory, endpointIdentifier, endpointScheduler,
                selfEndpoint, generateId(selfId, idLen), null);

        selfEndpoint.send(NullEndpoint.INSTANCE, start);
    }
    
    private static <T> void linkToGatewayAndBootstrapStart(
            ChordActiveListener<Id> activeListener, ChordLinkListener<Id> linkListener, ChordUnlinkListener<Id> unlinkListener,
            int selfId, int idLen, T joinAddress, Endpoint selfEndpoint, Gateway<T> gateway, EndpointScheduler endpointScheduler) {
        EndpointDirectory<T> endpointDirectory = new GatewayEndpointDirectory<>(gateway);
        EndpointIdentifier<T> endpointIdentifier = new GatewayEndpointIdentifier<>();
        
        
        Start<T> start = new Start<>(activeListener, linkListener, unlinkListener, endpointDirectory, endpointIdentifier, endpointScheduler,
                selfEndpoint, generateId(selfId, idLen), joinAddress);

        selfEndpoint.send(NullEndpoint.INSTANCE, start);
    }
    
    private static <T> void showNode(Visualizer<Id> visualizer, Id id, Mode mode) {
        Point position = VisualizerUtils.pointOnCircle(600.0,
                new BigDecimal(new BigInteger(id.getValueAsByteArray()))
                .divide(new BigDecimal(new BigInteger(id.getLimitAsByteArray()).add(BigInteger.ONE)))
                .doubleValue());
        visualizer.step("Creating node " + id + " as " + mode,
                new AddNodeCommand<>(id),
                new ChangeNodeCommand<>(id, 1.0, position, Color.LIGHT_GRAY));
    }
    
    private static Id generateId(int id, int len) {
        return new Id(new BigInteger("" + id).toByteArray(), new BigInteger("" + (len - 1)).toByteArray());
    }
}
