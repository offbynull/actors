package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.gateways.visualizer.AddNode;
import com.offbynull.peernetic.gateways.visualizer.GraphGateway;
import com.offbynull.peernetic.gateways.visualizer.MoveNode;
import com.offbynull.peernetic.gateways.visualizer.PositionUtils;
import com.offbynull.peernetic.gateways.visualizer.StyleNode;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
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
        
        
        
        int bits = 4;
        int count = (1 << bits) - 1;
        
        for (int i = 0; i <= count; i++) {
            NodeId selfId = new NodeId(i, bits);
            BigDecimal idDec = new BigDecimal(selfId.getValueAsBigInteger());
            BigDecimal limitDec = new BigDecimal(selfId.getLimitAsBigInteger()).add(BigDecimal.ONE);
            double percentage = idDec.divide(limitDec, 10, RoundingMode.FLOOR).doubleValue();
            Point newPoint = PositionUtils.pointOnCircle(500, percentage);
            graphGateway.getIncomingShuttle().send(Arrays.asList(
                    new Message("", "graph", new AddNode(selfId.toString())),
                    new Message("", "graph", new MoveNode(selfId.toString(), newPoint.getX(), newPoint.getY())),
                    new Message("", "graph", new StyleNode(selfId.toString(), "-fx-background-color: red"))
            ));
        }
        
        graphGateway.addStage(() -> new ControllerStage(actorThread, bits));
        
        
//        actorThread.addCoroutineActor("0", new ChordClientCoroutine(), new Start(new NodeId(0, bits), new Random(0), "timer", "graph"));
//        for (int i = 1; i <= count; i++) {
//            String id = Integer.toString(i);
//            actorThread.addCoroutineActor(id, new ChordClientCoroutine(),
//                    new Start("actor:" + mainRandom.nextInt(i), new NodeId(i, bits), new Random(i), "timer", "graph"));
//            
//            Thread.sleep(1000L);
//        }
        
        GraphGateway.awaitShutdown();
    }
}
