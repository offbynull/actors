package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.ReplayerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.simulator.MessageSink;
import com.offbynull.peernetic.core.simulator.RecordMessageSink;
import com.offbynull.peernetic.core.simulator.Simulator;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.PositionUtils;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.awt.Point;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class SimulationDirect {

    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    private static final Address EMPTY_ADDRESS = Address.of();
    
    private static final int NUM_NODES = 8;
    
    public static void main(String[] args) throws Exception {
        // Create simulator
        Instant time = Instant.ofEpochMilli(0L);
        Simulator simulator = new Simulator(time);

        // Instruct simulator to save messages to graph
        File tempFile = File.createTempFile(SimulationDirect.class.getSimpleName(), ".graphmsgs");
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
        addNode(0, null, bits, simulator, time);
        for (int i = 1; i < NUM_NODES; i++) {
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
        
           // Manually add nodes to graph before replaying
        int graphRadius = Math.max(300, NUM_NODES * 4);
        for (int i = 0; i < NUM_NODES; i++) {
            addNodeToGraph(i, bits, graphRadius, graphGateway);
        }
        
          // Replay
        ReplayerGateway replayerGateway = ReplayerGateway.replay(
                graphGateway.getIncomingShuttle(),
                BASE_GRAPH_ADDRESS,
                tempFile,
                new SimpleSerializer());

        replayerGateway.await();
        GraphGateway.awaitShutdown();
    }

    private static void addNodeToGraph(int id, int bits, int graphRadius, GraphGateway graphGateway) {
        NodeId selfId = new NodeId(id, bits);
        String selfIdStr = selfId.toString();
        
        BigDecimal idDec = new BigDecimal(selfId.getValueAsBigInteger());
        BigDecimal limitDec = new BigDecimal(selfId.getLimitAsBigInteger()).add(BigDecimal.ONE);
        double percentage = idDec.divide(limitDec, 10, RoundingMode.FLOOR).doubleValue();
        
        Point newPoint = PositionUtils.pointOnCircle(graphRadius, percentage);
        
        graphGateway.getIncomingShuttle().send(Arrays.asList(
                new Message(BASE_GRAPH_ADDRESS, BASE_GRAPH_ADDRESS, new AddNode(selfIdStr)),
                new Message(BASE_GRAPH_ADDRESS, BASE_GRAPH_ADDRESS, new MoveNode(selfIdStr, newPoint.getX(), newPoint.getY())),
                new Message(BASE_GRAPH_ADDRESS, BASE_GRAPH_ADDRESS, new StyleNode(selfIdStr, "-fx-background-color: red"))
        ));
    }

    private static void addNode(int id, Integer connId, int bits, Simulator simulator, Instant time) {
        String idStr = Integer.toString(id);
        String connIdStr = connId == null ? null : connId.toString();

        simulator.addCoroutineActor(
                idStr,
                new ChordClientCoroutine(),
                Duration.ZERO,
                time,
                new Start(
                        new SimpleAddressTransformer(EMPTY_ADDRESS, idStr),
                        connIdStr,
                        new NodeId(id, bits),
                        id,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
}
