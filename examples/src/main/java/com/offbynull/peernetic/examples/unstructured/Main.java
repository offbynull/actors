package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.gateways.visualizer.GraphGateway;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import java.util.Random;

public final class Main {
    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();
        
        GraphGateway graphGateway = new GraphGateway("graph");
        TimerGateway timerGateway = new TimerGateway("timer");
        ActorThread actorThread = ActorThread.create("actor");
        
        timerGateway.addOutgoingShuttle(actorThread.getIncomingShuttle());
        actorThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(graphGateway.getIncomingShuttle());
        
        
        actorThread.addCoroutineActor("1", new UnstructuredClientCoroutine(), new Start(new Random(), "timer", "graph"));
        actorThread.addCoroutineActor("2", new UnstructuredClientCoroutine(), new Start("actor:1", new Random(), "timer", "graph"));
        actorThread.addCoroutineActor("3", new UnstructuredClientCoroutine(), new Start("actor:1", new Random(), "timer", "graph"));
        actorThread.addCoroutineActor("4", new UnstructuredClientCoroutine(), new Start("actor:1", new Random(), "timer", "graph"));
        actorThread.addCoroutineActor("5", new UnstructuredClientCoroutine(), new Start("actor:1", new Random(), "timer", "graph"));
        actorThread.addCoroutineActor("6", new UnstructuredClientCoroutine(), new Start("actor:1", new Random(), "timer", "graph"));
        actorThread.addCoroutineActor("7", new UnstructuredClientCoroutine(), new Start("actor:1", new Random(), "timer", "graph"));
        
        GraphGateway.awaitShutdown();
    }
}
