package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.gateways.visualizer.GraphGateway;
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
        
        
        
        Random mainRandom = new Random(0L);
        
        int bits = 7;
        int count = (1 << bits) - 1;
        
        
        actorThread.addCoroutineActor("0", new ChordClientCoroutine(), new Start(new NodeId(0, bits), new Random(0), "timer", "graph"));
        for (int i = 1; i <= count; i++) {
            String id = Integer.toString(i);
            actorThread.addCoroutineActor(id, new ChordClientCoroutine(),
                    new Start("actor:" + mainRandom.nextInt(i), new NodeId(i, bits), new Random(i), "timer", "graph"));
            
            Thread.sleep(1000L);
        }
        
        GraphGateway.awaitShutdown();
    }
}
