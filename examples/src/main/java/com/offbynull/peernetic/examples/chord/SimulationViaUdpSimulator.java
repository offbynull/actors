package com.offbynull.peernetic.examples.chord;

import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.ReplayerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.MessageSink;
import com.offbynull.peernetic.core.simulator.RecordMessageSink;
import com.offbynull.peernetic.core.simulator.Simulator;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.network.actors.udpsimulator.SimpleLine;
import com.offbynull.peernetic.network.actors.udpsimulator.StartUdpSimulator;
import com.offbynull.peernetic.network.actors.udpsimulator.UdpSimulatorCoroutine;
import com.offbynull.peernetic.visualizer.gateways.graph.DefaultNodeRemoveHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class SimulationViaUdpSimulator {

    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final String SIMULATED_UDP_PROXY_ID_FORMAT = "unrel%d";
    
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    private static final int NUM_NODES = 8;
    
    public static void main(String[] args) throws Exception {
        // Create simulator
        Instant time = Instant.ofEpochMilli(0L);
        Simulator simulator = new Simulator(time);

        // Instruct simulator to save messages to graph
        File tempFile = File.createTempFile(SimulationViaUdpSimulator.class.getSimpleName(), ".graphmsgs");
        MessageSink sink = new RecordMessageSink(BASE_GRAPH_ADDRESS_STRING, tempFile, new SimpleSerializer());
        simulator.addMessageSink(sink, time); // add sink
        simulator.addCoroutineActor(          // add fake actor for "graph" so sink above actually gets sent msgs by simulator --
                BASE_GRAPH_ADDRESS_STRING,     // simulator will not send messagesto sink if actor isn't present
                cnt -> {
                    while (true) {
                        cnt.suspend();
                    }
                },
                Duration.ZERO,
                time);

        // Instruct simulator to add timer
        simulator.addTimer(BASE_TIMER_ADDRESS_STRING, time);

        // Calculate ring size in bits
        Validate.isTrue(NUM_NODES > 0, "Bad size");
        Validate.isTrue(Integer.bitCount(NUM_NODES) == 1, "Not power of 2");
        int bits = Integer.numberOfTrailingZeros(NUM_NODES); // For example: 16 is 1000b, 1000b has 3 trailing zeros, so number of bits for
                                                             // ring-space is 3. Nodes will start from 0 to 15.
        
        // Instruct simulator to add nodes
        addUdpSimulatorProxy(0, simulator, time, 0);
        addNode(0, null, bits, simulator, time);
        for (int i = 1; i < NUM_NODES; i++) {
            addUdpSimulatorProxy(i, simulator, time, i);
            addNode(i, 0, bits, simulator, time);
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
        
        graphGateway.setHandlers(new CustomGraphNodeAddHandler(NUM_NODES), new DefaultNodeRemoveHandler());
        
          // Replay
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
        String udpSimProxyIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        
        simulator.addCoroutineActor(
                udpSimProxyIdStr,
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

    private static void addNode(int id, Integer connId, int bits, Simulator simulator, Instant time) {
        String idStr = Integer.toString(id);
        String udpSimProxyIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        Address remoteBaseAddr = Address.of(udpSimProxyIdStr);
        String connIdStr = connId == null ? null : String.format(SIMULATED_UDP_PROXY_ID_FORMAT, connId);

        byte[] seed = new byte[MIN_SEED_SIZE];
        seed[0] = (byte) id;
        
        simulator.addCoroutineActor(
                idStr,
                new ChordClientCoroutine(),
                Duration.ZERO,
                time,
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
}
