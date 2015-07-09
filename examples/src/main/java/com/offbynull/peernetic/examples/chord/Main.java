package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.examples.common.ConsoleStage;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.common.SimpleAddressTransformer;
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

    private static final String BASE_ACTOR_ADDRESS_STRING = "actor";
    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final Address BASE_ACTOR_ADDRESS = Address.of(BASE_ACTOR_ADDRESS_STRING);
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();

        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);
        LogGateway logGateway = new LogGateway(BASE_LOG_ADDRESS_STRING);
        TimerGateway timerGateway = new TimerGateway(BASE_TIMER_ADDRESS_STRING);
        ActorThread actorThread = ActorThread.create(BASE_ACTOR_ADDRESS_STRING);

        timerGateway.addOutgoingShuttle(actorThread.getIncomingShuttle());
        actorThread.addOutgoingShuttle(logGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorThread.addOutgoingShuttle(graphGateway.getIncomingShuttle());

        graphGateway.addStage(() -> new ConsoleStage());
        ConsoleStage consoleStage = ConsoleStage.getInstance();

        ArrayBlockingQueue<Integer> sizeContainer = new ArrayBlockingQueue<>(1);
        consoleStage.outputLine("Enter size of Chord ring (must be power of 2)");
        consoleStage.setCommandProcessor((input) -> {
            sizeContainer.add(Integer.parseInt(input));
            return "Creating " + input + " nodes";
        });
        int size = sizeContainer.take();
        
        Validate.isTrue(size > 0, "Bad size");
        Validate.isTrue(Integer.bitCount(size) == 1, "Not power of 2");

        int bits = Integer.numberOfTrailingZeros(size); // For example: 16 is 1000b, 1000b has 3 trailing zeros, so number of bits for
        // ring-space is 3. Nodes will start from 0 to 15.

        int graphRadius = Math.max(300, size * 4);
        for (int i = 0; i < size; i++) {
            NodeId selfId = new NodeId(i, bits);
            BigDecimal idDec = new BigDecimal(selfId.getValueAsBigInteger());
            BigDecimal limitDec = new BigDecimal(selfId.getLimitAsBigInteger()).add(BigDecimal.ONE);
            double percentage = idDec.divide(limitDec, 10, RoundingMode.FLOOR).doubleValue();
            Point newPoint = PositionUtils.pointOnCircle(graphRadius, percentage);
            graphGateway.getIncomingShuttle().send(Arrays.asList(
                    new Message(Address.of(""), Address.of("graph"), new AddNode(selfId.toString())),
                    new Message(Address.of(""), Address.of("graph"), new MoveNode(selfId.toString(), newPoint.getX(), newPoint.getY())),
                    new Message(Address.of(""), Address.of("graph"), new StyleNode(selfId.toString(), "-fx-background-color: red"))
            ));
        }

        consoleStage.outputLine("Node colors");
        consoleStage.outputLine("-----------");
        consoleStage.outputLine("Red = Not active");
        consoleStage.outputLine("Yellow = Initializing");
        consoleStage.outputLine("Green = Active");
        consoleStage.outputLine("");
        consoleStage.outputLine("Line colors");
        consoleStage.outputLine("-----------");
        consoleStage.outputLine("Red = Finger");
        consoleStage.outputLine("Green = Successor");
        consoleStage.outputLine("Blue = Predecessor");
        consoleStage.outputLine("Yellow (Red+Green) = Finger+Successor");
        consoleStage.outputLine("Purple (Red+Blue) = Finger+Predecessor");
        consoleStage.outputLine("White (Red+Green+Blue) = Finger+Successor+Predecessor");
        consoleStage.outputLine("");
        consoleStage.outputLine("Available commands");
        consoleStage.outputLine("------------------");
        consoleStage.outputLine("Bootstrap node: boot <id>");
        consoleStage.outputLine("Connect nodes: start <start_id> <end_id> <connect_id>");
        consoleStage.outputLine("Stop nodes: stop <start_id> <end_id>");
        consoleStage.outputLine("To shutdown: exit");
        consoleStage.outputLine("");

        consoleStage.setCommandProcessor((input) -> {
            Scanner scanner = new Scanner(input);
            scanner.useDelimiter("\\s+");

            switch (scanner.next().toLowerCase()) {
                case "boot": {
                    int id = scanner.nextInt();
                    String idStr = Integer.toString(id);
                    actorThread.addCoroutineActor(idStr, new ChordClientCoroutine(),
                            new Start(
                                    new SimpleAddressTransformer(BASE_ACTOR_ADDRESS, idStr),
                                    null,
                                    new NodeId(id, bits),
                                    id,
                                    BASE_TIMER_ADDRESS,
                                    BASE_GRAPH_ADDRESS,
                                    BASE_LOG_ADDRESS
                            )
                    );
                    return "Executed command: " + input;
                }
                case "start": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();
                    int connectId = scanner.nextInt();

                    Validate.isTrue(startId <= endId);

                    String connectIdStr = Integer.toString(connectId);
                    for (int id = startId; id <= endId; id++) {
                        String idStr = Integer.toString(id);
                        actorThread.addCoroutineActor(idStr, new ChordClientCoroutine(),
                                new Start(
                                        new SimpleAddressTransformer(BASE_ACTOR_ADDRESS, idStr),
                                        connectIdStr,
                                        new NodeId(id, bits),
                                        id,
                                        BASE_TIMER_ADDRESS,
                                        BASE_GRAPH_ADDRESS,
                                        BASE_LOG_ADDRESS
                                )
                        );
                    }
                    return "Executed command: " + input;
                }
                case "stop": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();
                    
                    Validate.isTrue(startId <= endId);
                    
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
