package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.peernetic.network.gateways.udp.UdpGateway;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;

public final class RealtimeViaUdp {

    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();

        GraphGateway graphGateway = new GraphGateway("graph");
        TimerGateway timerGateway = new TimerGateway("timer");
        LogGateway logGateway = new LogGateway("log");
        ActorThread actorThread = ActorThread.create("actor");

        timerGateway.addOutgoingShuttle(actorThread.getIncomingShuttle());
        actorThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(graphGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(logGateway.getIncomingShuttle());

        Random rand = new Random(12345);

        addSeedNode(actorThread);
        for (int i = 1; i < 100; i++) {
            addNode(i, actorThread, rand);

            Thread.sleep(rand.nextInt(1000));
        }

        GraphGateway.awaitShutdown();
    }

    private static void addNode(int i, ActorThread actorThread, Random rand) throws Exception {
        String id = Integer.toString(i);
        
        UdpGateway udpGateway = new UdpGateway(
                new InetSocketAddress("127.0.0.1", 10000 + i),
                "udp" + id,
                actorThread.getIncomingShuttle(),
                Address.of("actor", id),
                new SimpleSerializer());
        actorThread.addOutgoingShuttle(udpGateway.getIncomingShuttle());
        actorThread.addCoroutineActor(
                id,
                new UnstructuredClientCoroutine(),
                new Start(
                        addr -> "7f000001." + (10000 + Integer.valueOf(addr.getElement(1))), // e.g. actor:0 -> 7f000001.10000
                        addr -> addr.getElement(1),                                          // e.g. udp0:7f000001.10001 -> 7f000001.10001
                        str -> Address.of("udp" + i, str),                                   // e.g. 7f000001.10001 -> udp0:7f000001.10001
                        Address.of("udp" + i, "7f000001." + (10000 + rand.nextInt(i))),
                        (long) i,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }

    private static void addSeedNode(ActorThread actorThread) throws Exception {
        UdpGateway udpGateway = new UdpGateway(
                new InetSocketAddress("127.0.0.1", 10000),
                "udp0",
                actorThread.getIncomingShuttle(),
                Address.of("actor", "0"),
                new SimpleSerializer());
        actorThread.addOutgoingShuttle(udpGateway.getIncomingShuttle());
        actorThread.addCoroutineActor(
                "0",
                new UnstructuredClientCoroutine(),
                new Start(
                        addr -> "7f000001." + (10000 + Integer.valueOf(addr.getElement(1))), // e.g. actor:0 -> 7f000001.10000
                        addr -> addr.getElement(1),                                          // e.g. udp0:7f000001.10001 -> 7f000001.10001
                        str -> Address.of("udp0", str),                                      // e.g. 7f000001.10001 -> udp0:7f000001.10001
                        10000L,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }
}
