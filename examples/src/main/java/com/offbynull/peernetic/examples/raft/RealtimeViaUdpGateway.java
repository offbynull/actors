package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.actor.ActorRunner;
import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.examples.common.ConsoleStage;
import com.offbynull.peernetic.examples.raft.internalmessages.Kill;
import com.offbynull.peernetic.examples.raft.internalmessages.StartClient;
import com.offbynull.peernetic.examples.raft.internalmessages.StartServer;
import com.offbynull.peernetic.network.gateways.udp.UdpGateway;
import com.offbynull.peernetic.visualizer.gateways.graph.DefaultGraphNodeRemoveHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

public final class RealtimeViaUdpGateway {

    private static final String BASE_ACTOR_ADDRESS_STRING = "actor";
    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final String BASE_UDP_ADDRESS_STRING_FORMAT = "udp%d";
    
    private static final Address BASE_ACTOR_ADDRESS = Address.of(BASE_ACTOR_ADDRESS_STRING);
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    private static final int START_PORT = 10000;
    private static final String LOCALHOST_HEX = "7f000001";
    private static final String LOCALHOST = "127.0.0.1";
    
    private static final int MIN_ELECTION_TIMEOUT = 1500;
    private static final int MAX_ELECTION_TIMEOUT = 3000;
    
    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();

        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);
        TimerGateway timerGateway = new TimerGateway(BASE_TIMER_ADDRESS_STRING);
        LogGateway logGateway = new LogGateway(BASE_LOG_ADDRESS_STRING);
        ActorRunner actorRunner = new ActorRunner(BASE_ACTOR_ADDRESS_STRING);

        timerGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(graphGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());

        graphGateway.addStage(() -> new ConsoleStage());
        ConsoleStage consoleStage = ConsoleStage.getInstance();
        
        ArrayBlockingQueue<Integer> sizeContainer = new ArrayBlockingQueue<>(1);
        consoleStage.outputLine("Enter size of RAFT cluster");
        consoleStage.setCommandProcessor((input) -> {
            sizeContainer.add(Integer.parseInt(input));
            return "";
        });
        int clusterSize = sizeContainer.take();
        Validate.isTrue(clusterSize > 0, "Bad size");
        
        graphGateway.setHandlers(new CustomGraphNodeAddHandler(clusterSize), new DefaultGraphNodeRemoveHandler());
        
        // Generate server ids
        Set<Integer> serverIds = new HashSet<>();
        for (int i = 0; i < clusterSize; i++) {
            serverIds.add(i);
        }
        
        // Output infoermation
        consoleStage.outputLine("Server nodes: " + serverIds);
        consoleStage.outputLine("Client node: " + clusterSize);
        consoleStage.outputLine("");
        
        // Start udp simulator proxy actor for servers and client
        for (int i = 0; i < clusterSize; i++) {
            addUdpGateway(i, actorRunner);
        }
        addUdpGateway(clusterSize, actorRunner);
        
        // Start client
        addClientNode(clusterSize, serverIds, actorRunner);
        
        // Take inputs
        consoleStage.outputLine("Node colors");
        consoleStage.outputLine("-----------");
        consoleStage.outputLine("Gray = Client");
        consoleStage.outputLine("Blue = Server (Follower)");
        consoleStage.outputLine("Yellow = Server (Candidate)");
        consoleStage.outputLine("Green = Server (Leader)");
        consoleStage.outputLine("");
        consoleStage.outputLine("Available commands");
        consoleStage.outputLine("------------------");
        consoleStage.outputLine("Start server node: start <start_id> <end_id>");
        consoleStage.outputLine("Stop server node: stop <start_id> <end_id>");
        consoleStage.outputLine("To shutdown: exit");
        consoleStage.outputLine("");
        
        consoleStage.setCommandProcessor((input) -> {
            Scanner scanner = new Scanner(input);
            scanner.useDelimiter("\\s+");

            switch (scanner.next().toLowerCase()) {
                case "start": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();

                    Validate.isTrue(startId < clusterSize);
                    Validate.isTrue(endId < clusterSize);
                    Validate.isTrue(startId <= endId);

                    for (int i = startId; i <= endId; i++) {
                        addServerNode(i, serverIds, actorRunner);
                    }
                    return "Executed command: " + input;
                }
                case "stop": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();

                    Validate.isTrue(startId <= endId);

                    for (int id = startId; id <= endId; id++) {
                        removeServerNode(id, actorRunner);
                    }
                    return "Executed command: " + input;
                }
                case "exit": {
                    GraphGateway.exitApplication();
                    return "Executed command: " + input;
                }
                default: {
                    return "Unknown command: " + input;
                }
            }
        });

        GraphGateway.awaitShutdown();
    }

    private static void addUdpGateway(int id, ActorRunner actorRunner) throws Exception {
        String idStr = Integer.toString(id);
        String baseUdpAddressStr = String.format(BASE_UDP_ADDRESS_STRING_FORMAT, id);
        int bindPort = START_PORT + id;
        InetSocketAddress bindAddress = new InetSocketAddress(LOCALHOST, bindPort);
        
        UdpGateway udpGateway = new UdpGateway(
                bindAddress,
                baseUdpAddressStr,
                actorRunner.getIncomingShuttle(),
                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                new SimpleSerializer()
        );
        actorRunner.addOutgoingShuttle(udpGateway.getIncomingShuttle());
    }
    
    private static void addClientNode(int clientId, Collection<Integer> allServerIds, ActorRunner actorRunner) {
        String idStr = Integer.toString(clientId);
        String selfLinkId = LOCALHOST_HEX + "." + (START_PORT + clientId);
        String baseUdpAddressStr = String.format(BASE_UDP_ADDRESS_STRING_FORMAT, clientId);
        Address baseUdpAddress = Address.of(baseUdpAddressStr);
        Set<String> allIdsAsStrs = allServerIds.stream()
                .map(x -> LOCALHOST_HEX + "." + (START_PORT + x))
                .collect(Collectors.toSet());
        
        actorRunner.addActor(
                idStr,
                new RaftClientCoroutine(),
                new StartClient(
                        new SimpleAddressTransformer(baseUdpAddress, selfLinkId),
                        MIN_ELECTION_TIMEOUT,
                        MAX_ELECTION_TIMEOUT,
                        allIdsAsStrs,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }

    private static void addServerNode(int serverId, Collection<Integer> allServerIds, ActorRunner actorRunner) {
        String idStr = Integer.toString(serverId);
        String selfLinkId = LOCALHOST_HEX + "." + (START_PORT + serverId);
        String baseUdpAddressStr = String.format(BASE_UDP_ADDRESS_STRING_FORMAT, serverId);
        Address baseUdpAddress = Address.of(baseUdpAddressStr);
        Set<String> allIdsAsStrs = allServerIds.stream()
                .map(x -> LOCALHOST_HEX + "." + (START_PORT + x))
                .collect(Collectors.toSet());
        
        byte[] seed = new byte[MIN_SEED_SIZE];
        seed[0] = (byte) serverId;
        
        actorRunner.addActor(
                idStr,
                new RaftServerCoroutine(),
                new StartServer(
                        new SimpleAddressTransformer(baseUdpAddress, selfLinkId),
                        MIN_ELECTION_TIMEOUT,
                        MAX_ELECTION_TIMEOUT,
                        allIdsAsStrs,
                        seed,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
    
    private static void removeServerNode(int id, ActorRunner actorRunner) {
        String idStr = Integer.toString(id);

        actorRunner.getIncomingShuttle().send(
                Collections.singleton(
                        new Message(
                                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                                new Kill()
                        )
                )
        );
    }
}
