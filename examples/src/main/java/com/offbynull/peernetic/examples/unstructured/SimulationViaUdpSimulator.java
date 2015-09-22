package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.ReplayerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.MessageSink;
import com.offbynull.peernetic.core.simulator.RecordMessageSink;
import com.offbynull.peernetic.core.simulator.Simulator;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.peernetic.network.actors.udpsimulator.SimpleLine;
import com.offbynull.peernetic.network.actors.udpsimulator.StartUdpSimulator;
import com.offbynull.peernetic.network.actors.udpsimulator.UdpSimulatorCoroutine;
import com.offbynull.peernetic.visualizer.gateways.graph.DefaultGraphNodeRemoveHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public final class SimulationViaUdpSimulator {

    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final String SIMULATED_UDP_PROXY_ID_FORMAT = "unrel%d";
    
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    private static final int MAX_NODES = 100;
    private static final int MAX_WAIT_PER_NODE_ADD = 1000; // in milliseconds
    private static final int MAX_GRAPH_X = 1000;
    private static final int MAX_GRAPH_Y = 1000;

    public static void main(String[] args) throws Exception {
        // Create simulator
        Instant time = Instant.ofEpochMilli(0L);
        Simulator simulator = new Simulator(time);

        // Instruct simulator to save messages to graph
        File tempFile = File.createTempFile(SimulationViaUdpSimulator.class.getSimpleName(), ".graphmsgs");
        MessageSink sink = new RecordMessageSink(BASE_GRAPH_ADDRESS_STRING, tempFile, new SimpleSerializer());
        simulator.addMessageSink(sink, time); // add sink
        simulator.addActor(                    // add fake actor for "graph" so sink above actually gets sent msgs by simulator --
                BASE_GRAPH_ADDRESS_STRING,     // simulator will not send messagesto sink if actor isn't present
                (Continuation cnt) -> {
                    while (true) {
                        cnt.suspend();
                    }
                },
                Duration.ZERO,
                time);

        // Instruct simulator to add timer
        simulator.addTimer(BASE_TIMER_ADDRESS_STRING, time);

        // Instruct simulator to add nodes
        Random rand = new Random(12345);
        addUdpSimulatorProxy(0, simulator, time, 0);
        addNode(0, null, simulator, time);
        for (int i = 1; i < MAX_NODES; i++) {
            time = time.plus(rand.nextInt(MAX_WAIT_PER_NODE_ADD), ChronoUnit.MILLIS);
            addUdpSimulatorProxy(i, simulator, time, i);
            addNode(i, 0, simulator, time);
        }

        // Run simulation  (NOTE: enabling logging slows this simulation to a crawl, use slf4j-nop for best performance)
        for (int i = 0; i < 20000 && simulator.hasMore(); i++) {
            simulator.process();
        }

        // Close sink
        sink.close();

        

        
        
        // Replay saved graph messages from simulation to a real graph
        GraphGateway.startApplication();
        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);
        graphGateway.setHandlers(new CustomGraphNodeAddHandler(MAX_GRAPH_X, MAX_GRAPH_Y), new DefaultGraphNodeRemoveHandler());
        ReplayerGateway replayerGateway = ReplayerGateway.replay(
                graphGateway.getIncomingShuttle(),
                BASE_GRAPH_ADDRESS,
                tempFile,
                new SimpleSerializer());

        replayerGateway.await();
        GraphGateway.awaitShutdown();
    }

    private static void addUdpSimulatorProxy(int id, Simulator simulator, Instant time, int randomSeed) {
        String idStr = Integer.toString(id);
        String unreliableIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        
        simulator.addActor(
                unreliableIdStr,
                new UdpSimulatorCoroutine(),
                Duration.ZERO,
                time,
                new StartUdpSimulator(
                        BASE_TIMER_ADDRESS,
                        Address.of(idStr),
                        () -> new SimpleLine(
                                randomSeed,
                                Duration.ofMillis(100L),
                                Duration.ofMillis(100L),
                                0.1,
                                0.1,
                                1,
                                16 * 1024,
                                new SimpleSerializer())
                )
        );
    }
    
    private static void addNode(int id, Integer connId, Simulator simulator, Instant time) {
        String idStr = Integer.toString(id);
        String unreliableIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        Address remoteBaseAddr = Address.of(unreliableIdStr);
        String connIdStr = connId == null ? null : String.format(SIMULATED_UDP_PROXY_ID_FORMAT, connId);

        byte[] seed = new byte[MIN_SEED_SIZE];
        seed[0] = (byte) id;

        simulator.addActor(
                idStr,
                new UnstructuredClientCoroutine(),
                Duration.ZERO,
                time,
                new Start(
                        new SimpleAddressTransformer(remoteBaseAddr, Address.of(idStr), unreliableIdStr),
                        connIdStr,
                        seed,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
}
