package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import java.util.Random;

public final class Realtime {

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

    private static void addNode(int i, ActorThread actorThread, Random rand) {
        String id = Integer.toString(i);
        
        actorThread.addCoroutineActor(
                id,
                new UnstructuredClientCoroutine(),
                new Start(
                        addr -> addr.getElement(1),      // e.g. actor:0 -> 0
                        addr -> addr.getElement(1),      // e.g. actor:0 -> 0
                        str -> Address.of("actor", str), // e.g. 0 -> actor:0
                        Address.of("actor", "" + rand.nextInt(i)),
                        (long) i,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }

    private static void addSeedNode(ActorThread actorThread) {
        actorThread.addCoroutineActor(
                "0",
                new UnstructuredClientCoroutine(),
                new Start(
                        addr -> addr.getElement(1),      // e.g. actor:0 -> 0
                        addr -> addr.getElement(1),      // e.g. actor:0 -> 0
                        str -> Address.of("actor", str), // e.g. 0 -> actor:0
                        Address.of("actor", "0"),
                        0L,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }
}
