package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import java.util.Random;

public final class Main {
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
        
        actorThread.addCoroutineActor(
                "0",
                new UnstructuredClientCoroutine(),
                new Start(0L, Address.of("timer"), Address.of("graph"), Address.of("log")));
        for (int i = 1; i < 10; i++) {
            String id = Integer.toString(i);
            String bootstrapAddress = "actor:" + rand.nextInt(i);
            
            actorThread.addCoroutineActor(id, new UnstructuredClientCoroutine(),
                    new Start(Address.fromString(bootstrapAddress), (long) i, Address.of("timer"), Address.of("graph"), Address.of("log")));
            
            Thread.sleep(1000L);
        }
        
        GraphGateway.awaitShutdown();
    }
}
