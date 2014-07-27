package com.offbynull.peernetic.demo;

import com.offbynull.peernetic.FsmActor;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.SimpleEndpointScheduler;
import com.offbynull.peernetic.debug.testnetwork.Hub;
import com.offbynull.peernetic.debug.testnetwork.HubEndpointDirectory;
import com.offbynull.peernetic.debug.testnetwork.HubEndpointIdentifier;
import com.offbynull.peernetic.debug.testnetwork.SimpleLine;
import com.offbynull.peernetic.debug.testnetwork.messages.JoinHub;
import com.offbynull.peernetic.debug.testnetwork.messages.StartHub;
import com.offbynull.peernetic.debug.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.debug.visualizer.AddNodeCommand;
import com.offbynull.peernetic.debug.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.debug.visualizer.JGraphXVisualizer;
import com.offbynull.peernetic.debug.visualizer.RemoveEdgeCommand;
import com.offbynull.peernetic.debug.visualizer.Visualizer;
import com.offbynull.peernetic.debug.visualizer.VisualizerUtils;
import com.offbynull.peernetic.demo.messages.internal.Start;
import com.offbynull.peernetic.network.XStreamSerializer;
import java.awt.Color;
import java.awt.Point;
import java.time.Duration;
import java.util.Collections;

public final class Main {

    public static void main(String[] args) throws Throwable {
        FsmActor[] actors = new FsmActor[400];

        // Start visualizer
        Visualizer<Integer> visualizer = new JGraphXVisualizer<>();
        visualizer.visualize();

        UnstructuredClientListener<Integer> listener = new UnstructuredClientListener<Integer>() {

            @Override
            public void onStarted(Integer id) {
                Point position = VisualizerUtils.randomPointInRectangle(1000, 1000);
                visualizer.step("Creating node " + id,
                        new AddNodeCommand<>(id),
                        new ChangeNodeCommand<>(id, 1.0, position, Color.LIGHT_GRAY));
            }

            @Override
            public void onOutgoingConnected(Integer from, Integer to) {
                visualizer.step("Connected " + from + " to " + to,
                        new AddEdgeCommand<>(from, to));
            }

            @Override
            public void onDisconnected(Integer from, Integer to) {
                visualizer.step("Disconnected " + from + " to " + to,
                        new RemoveEdgeCommand<>(from, to));
            }
        };
        
        
        EndpointScheduler endpointScheduler = new SimpleEndpointScheduler();
        
        // Create and start fake network hub
        Hub<Integer> hub = new Hub<>();
        Actor hubActor = new FsmActor(hub, Hub.INITIAL_STATE);
        ActorRunnable hubActorRunnable = ActorRunnable.createAndStart(hubActor);
        Endpoint hubEndpoint = hubActorRunnable.getEndpoint(hubActor);
        
        hubEndpoint.send(NullEndpoint.INSTANCE, new StartHub<>(
                endpointScheduler,
                new SimpleLine(0L, Duration.ofMillis(500L), Duration.ofMillis(100L), 0.1, 0.9, 10),
                new XStreamSerializer(),
                hubEndpoint));
        
        
        // Create actors
        for (int i = 0; i < actors.length; i++) {
            UnstructuredClient<Integer> unstructuredClient = new UnstructuredClient<>(listener);
            actors[i] = new FsmActor(unstructuredClient, UnstructuredClient.INITIAL_STATE);
        }

        // Start actors all within the same thread
        ActorRunnable actorRunnable = ActorRunnable.createAndStart(actors);
        
        
        // Tell the hub about each actor
        for (int i = 0; i < actors.length; i++) {
            Endpoint endpoint = actorRunnable.getEndpoint(actors[i]);
            hubEndpoint.send(endpoint, new JoinHub<>(i));
        }

        
        // Start actors
        for (int i = 0; i < actors.length; i++) {
            Endpoint endpoint = actorRunnable.getEndpoint(actors[i]);
            HubEndpointDirectory<Integer> endpointDirectory = new HubEndpointDirectory<>(i, hubEndpoint);
            HubEndpointIdentifier<Integer> endpointIdentifier = new HubEndpointIdentifier<>();
            
            Start<Integer> start = new Start<>(endpointDirectory, endpointIdentifier, endpointScheduler, Collections.singleton(0),
                    endpoint, i);
            endpoint.send(NullEndpoint.INSTANCE, start);
        }
    }
}
