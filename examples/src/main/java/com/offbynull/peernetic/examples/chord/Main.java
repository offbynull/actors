package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.PositionUtils;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.commons.lang3.Validate;

public final class Main {

    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();

        GraphGateway graphGateway = new GraphGateway("graph");
        LogGateway logGateway = new LogGateway("log");
        TimerGateway timerGateway = new TimerGateway("timer");
        ActorThread actorThread = ActorThread.create("actor");

        timerGateway.addOutgoingShuttle(actorThread.getIncomingShuttle());
        actorThread.addOutgoingShuttle(logGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(graphGateway.getIncomingShuttle());

        graphGateway.addStage(() -> new ConsoleStage());
        ConsoleStage consoleStage = ConsoleStage.getInstance();

        ArrayBlockingQueue<Integer> sizeContainer = new ArrayBlockingQueue<>(1);
        consoleStage.outputLine("Enter size of ring (must be power of 2)");
        consoleStage.setCommandProcessor((input) -> {
            sizeContainer.add(Integer.parseInt(input));
            return "Creating " + input + " nodes";
        });
        int size = sizeContainer.take();
        
        Validate.isTrue(size > 0, "Negative size");
        Validate.isTrue(Integer.bitCount(size) == 1, "Not power of 2");

        int bits = Integer.numberOfTrailingZeros(size); // For example: 16 is 1000b, 1000b has 3 trailing zeros, so number of bits for
        // ring-space is 3. Nodes will start from 0 to 15.

        for (int i = 0; i < size; i++) {
            NodeId selfId = new NodeId(i, bits);
            BigDecimal idDec = new BigDecimal(selfId.getValueAsBigInteger());
            BigDecimal limitDec = new BigDecimal(selfId.getLimitAsBigInteger()).add(BigDecimal.ONE);
            double percentage = idDec.divide(limitDec, 10, RoundingMode.FLOOR).doubleValue();
            Point newPoint = PositionUtils.pointOnCircle(500, percentage);
            graphGateway.getIncomingShuttle().send(Arrays.asList(
                    new Message(Address.of(""), Address.of("graph"), new AddNode(selfId.toString())),
                    new Message(Address.of(""), Address.of("graph"), new MoveNode(selfId.toString(), newPoint.getX(), newPoint.getY())),
                    new Message(Address.of(""), Address.of("graph"), new StyleNode(selfId.toString(), "-fx-background-color: red"))
            ));
        }


        consoleStage.outputLine("Available commands");
        consoleStage.outputLine("------------------");
        consoleStage.outputLine("Bootstrap node: boot <id>");
        consoleStage.outputLine("Connect nodes: start <start_id> <end_id> <connect_id>");
        consoleStage.outputLine("Stop nodes: stop <start_id> <end_id>");
        consoleStage.outputLine("To shutdown: exit");
        consoleStage.outputLine("");
        consoleStage.outputLine("For example, to add node 0 as the inital network in a node and then have nodes 1 to 10 join it use the "
                + "following commands...");
        consoleStage.outputLine("boot 0");
        consoleStage.outputLine("start 1 10 0");
        consoleStage.outputLine("");

        consoleStage.setCommandProcessor((input) -> {
            Scanner scanner = new Scanner(input);
            scanner.useDelimiter("\\s+");

            switch (scanner.next().toLowerCase()) {
                case "boot": {
                    int id = scanner.nextInt();
                    actorThread.addCoroutineActor("" + id, new ChordClientCoroutine(),
                            new Start(
                                    null,
                                    new NodeId(id, bits),
                                    id,
                                    Address.fromString("timer"),
                                    Address.fromString("graph"),
                                    Address.fromString("log")
                            )
                    );
                    return "Executed command: " + input;
                }
                case "start": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();
                    int connectId = scanner.nextInt();

                    Validate.isTrue(startId <= endId);

                    for (int id = startId; id <= endId; id++) {
                        actorThread.addCoroutineActor("" + id, new ChordClientCoroutine(),
                                new Start(
                                        Address.of("actor", "" + connectId),
                                        new NodeId(id, bits),
                                        id,
                                        Address.fromString("timer"),
                                        Address.fromString("graph"),
                                        Address.fromString("log")
                                )
                        );
                    }
                    return "Executed command: " + input;
                }
                case "stop": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();
                    for (int id = startId; id <= endId; id++) {
                        actorThread.getIncomingShuttle().send(
                                Collections.singleton(
                                        new Message(
                                                Address.of("actor", "" + id),
                                                Address.of("actor", "" + id),
                                                new Kill()
                                        )
                                )
                        );
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
}
