package com.offbynull.peernetic.demo;

import com.offbynull.peernetic.FsmActor;
import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.SimpleEndpointDirectory;
import com.offbynull.peernetic.actor.SimpleEndpointIdentifier;
import com.offbynull.peernetic.actor.SimpleEndpointScheduler;
import com.offbynull.peernetic.debug.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.debug.visualizer.AddNodeCommand;
import com.offbynull.peernetic.debug.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.debug.visualizer.JGraphXVisualizer;
import com.offbynull.peernetic.debug.visualizer.RemoveEdgeCommand;
import com.offbynull.peernetic.debug.visualizer.Visualizer;
import com.offbynull.peernetic.debug.visualizer.VisualizerUtils;
import com.offbynull.peernetic.demo.messages.internal.Start;
import java.awt.Color;
import java.awt.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Main {

    public static void main(String[] args) throws Throwable {
        FsmActor[] actors = new FsmActor[100];

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

        
        // Create actors
        for (int i = 0; i < actors.length; i++) {
            UnstructuredClient<Integer> unstructuredClient = new UnstructuredClient<>(listener);
            actors[i] = new FsmActor(unstructuredClient, UnstructuredClient.INITIAL_STATE);
        }

        // Start actors all within the same thread
        ActorRunnable actorRunnable = ActorRunnable.createAndStart(actors);

        
        // Get endpoints for actors and create identifier+directory
        Map<Endpoint, Integer> endpointsToIds = new HashMap<>();
        Map<Integer, Endpoint> idsToEndpoints = new HashMap<>();
        for (int i = 0; i < actors.length; i++) {
            Endpoint endpoint = actorRunnable.getEndpoint(actors[i]);
            endpointsToIds.put(endpoint, i);
            idsToEndpoints.put(i, endpoint);
        }

        SimpleEndpointDirectory<Integer> endpointDirectory = new SimpleEndpointDirectory<>(idsToEndpoints);
        SimpleEndpointIdentifier<Integer> endpointIdentifier = new SimpleEndpointIdentifier<>(endpointsToIds);

        
        // Send start signal to nodes
        EndpointScheduler endpointScheduler = new SimpleEndpointScheduler();
        for (int i = 0; i < actors.length; i++) {
            Endpoint endpoint = actorRunnable.getEndpoint(actors[i]);
            Start<Integer> start = new Start<>(endpointDirectory, endpointIdentifier, endpointScheduler, Collections.singleton(0), endpoint);
            endpoint.send(NullEndpoint.INSTANCE, start);
        }
    }
}
