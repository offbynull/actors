package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.common.SimpleAddressTransformer;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.peernetic.network.gateways.udp.UdpGateway;
import java.net.InetSocketAddress;
import java.util.Random;

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
    
    private static final int MAX_NODES = 100;
    private static final int MAX_WAIT_PER_NODE_ADD = 1000; // in milliseconds
    
    private static final int START_PORT = 10000;
    private static final String LOCALHOST_HEX = "7f000001";
    private static final String LOCALHOST = "127.0.0.1";

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

        Random rand = new Random(12345);

        // Seed node
        addUdpGateway(0, actorThread);
        addNode(0, null, actorThread);
        
        // Connecting nodes
        for (int i = 1; i < MAX_NODES; i++) {
            addUdpGateway(i, actorThread);
            addNode(i, 0, actorThread);
            Thread.sleep(rand.nextInt(MAX_WAIT_PER_NODE_ADD));
        }

        GraphGateway.awaitShutdown();
    }

    private static void addUdpGateway(int id, ActorThread actorThread) throws Exception {
        String idStr = Integer.toString(id);
        String baseUdpAddressStr = String.format(BASE_UDP_ADDRESS_STRING_FORMAT, id);
        int bindPort = START_PORT + id;
        InetSocketAddress bindAddress = new InetSocketAddress(LOCALHOST, bindPort);
        
        UdpGateway udpGateway = new UdpGateway(
                bindAddress,
                baseUdpAddressStr,
                actorThread.getIncomingShuttle(),
                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                new SimpleSerializer()
        );
        actorThread.addOutgoingShuttle(udpGateway.getIncomingShuttle());
    }
    
    private static void addNode(int id, Integer connId, ActorThread actorThread) throws Exception {
        String idStr = Integer.toString(id);
        String selfLinkId = LOCALHOST_HEX + "." + (START_PORT + id);
        String baseUdpAddressStr = String.format(BASE_UDP_ADDRESS_STRING_FORMAT, id);
        Address baseUdpAddress = Address.of(baseUdpAddressStr);
        Address connIdAddr = null;
        if (connId != null) {
            int connIdPort = START_PORT + connId;
            connIdAddr = baseUdpAddress.appendSuffix(LOCALHOST_HEX + "." + connIdPort);
        }

        
        actorThread.addCoroutineActor(
                idStr,
                new UnstructuredClientCoroutine(),
                new Start(
                        new SimpleAddressTransformer(baseUdpAddress, selfLinkId),
                        connIdAddr,
                        (long) id,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
}
