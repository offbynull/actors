package com.offbynull.peernetic.playground.unstructuredmesh;

import com.offbynull.peernetic.GatewayEndpointDirectory;
import com.offbynull.peernetic.GatewayEndpointIdentifier;
import com.offbynull.peernetic.GatewayInputAdapter;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.SimpleEndpointScheduler;
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
import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.network.Gateway;
import com.offbynull.peernetic.network.GatewayListener;
import com.offbynull.peernetic.network.XStreamSerializer;
import com.offbynull.peernetic.playground.unstructuredmesh.messages.internal.Start;
import java.awt.Color;
import java.awt.Point;
import java.time.Duration;
import java.util.Collections;

public final class Main {

    public static void main(String[] args) throws Throwable {
        Actor[] actors = new Actor[200];

        // Start visualizer
        Visualizer<Object> visualizer = new JGraphXVisualizer<>();
        visualizer.visualize();

        UnstructuredClientListener<? extends Object> listener = new UnstructuredClientListener<Object>() {

            @Override
            public void onStarted(Object id) {
                Point position = VisualizerUtils.randomPointInRectangle(1000, 1000);
                visualizer.step("Creating node " + id,
                        new AddNodeCommand<>(id),
                        new ChangeNodeCommand<>(id, 1.0, position, Color.LIGHT_GRAY));
            }

            @Override
            public void onOutgoingConnected(Object from, Object to) {
                visualizer.step("Connected " + from + " to " + to,
                        new AddEdgeCommand<>(from, to));
            }

            @Override
            public void onDisconnected(Object from, Object to) {
                visualizer.step("Disconnected " + from + " to " + to,
                        new RemoveEdgeCommand<>(from, to));
            }
        };

        // Create actors
        for (int i = 0; i < actors.length; i++) {
            UnstructuredClient<? extends Object> client = new UnstructuredClient<>(listener);
            actors[i] = new JavaflowActor(client);
        }

        // Start actors all within the same thread
        ActorRunnable actorRunnable = ActorRunnable.createAndStart(actors);

        // UNCOMMENT THIS BLOCK to start each actor on the test network
        EndpointScheduler endpointScheduler = new SimpleEndpointScheduler();
        LocalGatewayHub<Integer> gatewayHub = new LocalGatewayHub<>(
                new SimpleLine<>(0L, Duration.ofMillis(500L), Duration.ofMillis(100L), 0.1, 0.9, 10),
                new XStreamSerializer());
        for (int i = 0; i < actors.length; i++) {
            Endpoint endpoint = actorRunnable.getEndpoint(actors[i]);

            int address = i;
            GatewayListener<Integer> gatewayListener = new GatewayInputAdapter<>(endpoint);
            LocalGateway<Integer> gateway = new LocalGateway<>(address, gatewayHub, gatewayListener);
            
            linkToGatewayAndStart(address, 0, endpoint, gateway, endpointScheduler);
        }

        
        
//        // UNCOMMENT THIS BLOCK TO start each actor on the real network
//        EndpointScheduler endpointScheduler = new SimpleEndpointScheduler();
//        for (int i = 0; i < actors.length; i++) {    
//            Endpoint endpoint = actorRunnable.getEndpoint(actors[i]);
//            
//            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
//            GatewayListener<InetSocketAddress> gatewayListener = new GatewayInputAdapter<>(endpoint);
//            UdpGateway gateway = new UdpGateway(address, gatewayListener, new XStreamSerializer());
//            
//            linkToGatewayAndStart(address, new InetSocketAddress(InetAddress.getLocalHost(), 10000), endpoint, gateway, endpointScheduler);
//        }
    }

    private static <T> void linkToGatewayAndStart(T selfAddress, T joinAddress, Endpoint selfEndpoint, Gateway<T> gateway,
            EndpointScheduler endpointScheduler) {
        EndpointDirectory<T> endpointDirectory = new GatewayEndpointDirectory<>(gateway);
        EndpointIdentifier<T> endpointIdentifier = new GatewayEndpointIdentifier<>();

        Start<T> start = new Start<>(endpointDirectory, endpointIdentifier, endpointScheduler, Collections.singleton(joinAddress),
                selfEndpoint, selfAddress);

        selfEndpoint.send(NullEndpoint.INSTANCE, start);
    }
}
