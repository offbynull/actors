package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.examples.raft.internalmessages.StartClient;
import com.offbynull.peernetic.examples.raft.internalmessages.StartServer;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class RealtimeDirect {

    private static final String BASE_ACTOR_ADDRESS_STRING = "actor";
    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final Address BASE_ACTOR_ADDRESS = Address.of(BASE_ACTOR_ADDRESS_STRING);
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    private static final int MAX_NODES = 2;
    
    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();

        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);
        TimerGateway timerGateway = new TimerGateway(BASE_TIMER_ADDRESS_STRING);
        LogGateway logGateway = new LogGateway(BASE_LOG_ADDRESS_STRING);
        ActorThread actorThread = ActorThread.create(BASE_ACTOR_ADDRESS_STRING);

        timerGateway.addOutgoingShuttle(actorThread.getIncomingShuttle());
        actorThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(graphGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(logGateway.getIncomingShuttle());

        
        // Generate server ids
        Set<Integer> serverIds = new HashSet<>();
        for (int i = 0; i < MAX_NODES; i++) {
            serverIds.add(i);
        }
        
        // Start servers
        for (int i = 0; i < MAX_NODES; i++) {
            addServerNode(i, serverIds, actorThread);
        }
        
        // Start clients
//        addClientNode(MAX_NODES, serverIds, actorThread);

        GraphGateway.awaitShutdown();
    }

    private static void addClientNode(int clientId, Collection<Integer> allServerIds, ActorThread actorThread) {
        String idStr = Integer.toString(clientId);
        Set<String> allIdsAsStrs = allServerIds.stream().map(x -> Integer.toString(x)).collect(Collectors.toSet());
        
        actorThread.addCoroutineActor(
                idStr,
                new RaftClientCoroutine(),
                new StartClient(
                        new SimpleAddressTransformer(BASE_ACTOR_ADDRESS, idStr),
                        allIdsAsStrs,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }

    private static void addServerNode(int serverId, Collection<Integer> allServerIds, ActorThread actorThread) {
        String idStr = Integer.toString(serverId);
        Set<String> allIdsAsStrs = allServerIds.stream().map(x -> Integer.toString(x)).collect(Collectors.toSet());
        
        actorThread.addCoroutineActor(
                idStr,
                new RaftServerCoroutine(),
                new StartServer(
                        new SimpleAddressTransformer(BASE_ACTOR_ADDRESS, idStr),
                        allIdsAsStrs,
                        (long) serverId,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
}
