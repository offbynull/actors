package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.examples.common.ConsoleStage;
import com.offbynull.peernetic.core.actor.ActorThread;
import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.network.actors.udpsimulator.SimpleLine;
import com.offbynull.peernetic.network.actors.udpsimulator.StartUdpSimulator;
import com.offbynull.peernetic.network.actors.udpsimulator.UdpSimulatorCoroutine;
import com.offbynull.peernetic.visualizer.gateways.graph.DefaultNodeRemoveHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.time.Duration;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.commons.lang3.Validate;

public final class RealtimeViaUdpSimulator {

    private static final String BASE_ACTOR_ADDRESS_STRING = "actor";
    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";

    private static final String SIMULATED_UDP_PROXY_ID_FORMAT = "unrel%d";

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

        graphGateway.setHandlers(new CustomGraphNodeAddHandler(size), new DefaultNodeRemoveHandler());

        for (int i = 0; i < size; i++) {
            addUdpSimulatorProxy(i, actorThread, i);
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
                    addNode(id, null, bits, actorThread);
                    return "Executed command: " + input;
                }
                case "start": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();
                    int connectId = scanner.nextInt();

                    Validate.isTrue(startId <= endId);

                    for (int id = startId; id <= endId; id++) {
                        addNode(id, connectId, bits, actorThread);
                    }
                    return "Executed command: " + input;
                }
                case "stop": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();

                    Validate.isTrue(startId <= endId);

                    for (int id = startId; id <= endId; id++) {
                        removeNode(id, actorThread);
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
    
    private static void addUdpSimulatorProxy(int id, ActorThread actorThread, int randomSeed) {
        String idStr = Integer.toString(id);
        String udpSimProxyIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        
        actorThread.addCoroutineActor(
                udpSimProxyIdStr,
                new UdpSimulatorCoroutine(),
                new StartUdpSimulator(
                        BASE_TIMER_ADDRESS,
                        BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                        () -> new SimpleLine(
                                randomSeed,
                                Duration.ofMillis(100L),
                                Duration.ofMillis(100L),
                                0.1,
                                0.1,
                                10,
                                16 * 1024,
                                new SimpleSerializer())
                )
        );
    }
    
    private static void addNode(int id, Integer connId, int bits, ActorThread actorThread) {
        String idStr = Integer.toString(id);
        String udpSimProxyIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        Address remoteBaseAddr = BASE_ACTOR_ADDRESS
                    .appendSuffix(udpSimProxyIdStr)
                    .appendSuffix(BASE_ACTOR_ADDRESS);
        String connIdStr = connId == null ? null : String.format(SIMULATED_UDP_PROXY_ID_FORMAT, connId);

        byte[] seed = new byte[MIN_SEED_SIZE];
        seed[0] = (byte) id;
        
        actorThread.addCoroutineActor(
                idStr,
                new ChordClientCoroutine(),
                new Start(
                        new SimpleAddressTransformer(remoteBaseAddr, udpSimProxyIdStr),
                        connIdStr,
                        new NodeId(id, bits),
                        seed,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }

    private static void removeNode(int id, ActorThread actorThread) {
        String idStr = Integer.toString(id);

        actorThread.getIncomingShuttle().send(
                Collections.singleton(
                        new Message(
                                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                                new Kill()
                        )
                )
        );
    }
}
