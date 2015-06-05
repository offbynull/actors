package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.peernetic.network.actors.udpsimulator.SimpleLine;
import com.offbynull.peernetic.network.actors.udpsimulator.StartUdpSimulator;
import com.offbynull.peernetic.network.actors.udpsimulator.UdpSimulatorCoroutine;
import java.time.Duration;
import java.util.Random;

public final class RealtimeWithUnreliable {

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

        addSeedNode(actorThread, rand);
        
        // other actors + unrealiable proxies for those actors
        for (int i = 1; i < 100; i++) {
            addNode(i, actorThread, rand);

            Thread.sleep(rand.nextInt(1000));
        }

        GraphGateway.awaitShutdown();
    }

    private static void addNode(int i, ActorThread actorThread, Random rand) {
        String id = Integer.toString(i);
        String unreliableId = "unrel" + Integer.toString(i);
        
        actorThread.addCoroutineActor(
                unreliableId,
                new UdpSimulatorCoroutine(),
                new StartUdpSimulator(
                        Address.of("timer"),
                        Address.of("actor", id),
                        () -> new SimpleLine(
                                rand.nextInt(),
                                Duration.ofMillis(100L),
                                Duration.ofMillis(100L),
                                0.1,
                                0.1,
                                1,
                                16 * 1024,
                                new SimpleSerializer())
                )
        );
        
        actorThread.addCoroutineActor(
                id,
                new UnstructuredClientCoroutine(),
                new Start(
                        addr -> addr.getElement(1),                                      // e.g. actor:0 -> 0
                        addr -> addr.getElement(3).substring(5),                         // e.g. actor:unrel0:actor:unrel1 -> 1
                        str -> Address.of("actor", "unrel" + i, "actor", "unrel" + str), // e.g. 1 -> actor:unrel0:actor:unrel1
                        Address.of("actor", "unrel" + i, "actor", "unrel" + rand.nextInt(i)), // bootstrap proxy thru unreliable
                                                                                              // e.g. actor:unrel0:actor:unrel1
                        (long) i,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }

    private static void addSeedNode(ActorThread actorThread, Random rand) {
        // initial actor + unreliable proxy
        actorThread.addCoroutineActor(
                "unrel0",
                new UdpSimulatorCoroutine(),
                new StartUdpSimulator(
                        Address.of("timer"),
                        Address.of("actor", "0"),
                        () -> new SimpleLine(
                                rand.nextInt(),
                                Duration.ofMillis(100L),
                                Duration.ofMillis(100L),
                                0.1,
                                0.1,
                                1,
                                16 * 1024,
                                new SimpleSerializer())
                )
        );
        
        actorThread.addCoroutineActor(
                "0",
                new UnstructuredClientCoroutine(),
                new Start(
                        addr -> addr.getElement(1),                                   // e.g. actor:0 -> 0
                        addr -> addr.getElement(3).substring(5),                      // e.g. actor:unrel0:actor:unrel1 -> 1
                        str -> Address.of("actor", "unrel0", "actor", "unrel" + str), // e.g. 1 -> actor:unrel0:actor:unrel1
                        0L,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }
}
