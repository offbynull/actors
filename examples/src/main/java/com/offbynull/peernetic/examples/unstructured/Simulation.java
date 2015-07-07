package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.ReplayerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.MessageSink;
import com.offbynull.peernetic.core.simulator.RecordMessageSink;
import com.offbynull.peernetic.core.simulator.Simulator;
import com.offbynull.peernetic.examples.common.SimpleAddressTransformer;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public final class Simulation {

    public static void main(String[] args) throws Exception {
        // Create simulator
        Instant time = Instant.ofEpochMilli(0L);
        Simulator simulator = new Simulator(time);

        // Instruct simulator to save messages to graph
        File tempFile = File.createTempFile(Simulation.class.getSimpleName(), ".graphmsgs");
        MessageSink sink = new RecordMessageSink("graph", tempFile, new SimpleSerializer());
        simulator.addMessageSink(sink, time); // add sink
        simulator.addCoroutineActor(          // add fake actor for "graph" so sink above actually gets sent msgs by simulator --
                "graph",                        // simulator will not send messagesto sink if actor isn't present
                cnt -> {
                    while (true) {
                        cnt.suspend();
                    }
                },
                Duration.ZERO,
                time);

        // Instruct simulator to add timer
        simulator.addTimer("timer", time);

        // Instruct simulator to add nodes
        Random rand = new Random(12345);
        addSeedNode(simulator, time);
        for (int i = 1; i < 100; i++) {
            time = time.plus(rand.nextInt(1000), ChronoUnit.MILLIS);
            addNode(i, simulator, rand, time);
        }

        // Run simulation  (NOTE: enabling logging slows this simulation to a crawl, use slf4j-nop for best performance)
        for (int i = 0; i < 20000 && simulator.hasMore(); i++) {
            simulator.process();
        }

        // Close sink
        sink.close();

        

        
        
        // Replay saved graph messages from simulation to a real graph
        GraphGateway.startApplication();
        GraphGateway graphGateway = new GraphGateway("graph");
        ReplayerGateway replayerGateway = ReplayerGateway.replay(
                graphGateway.getIncomingShuttle(),
                Address.of("graph"),
                tempFile,
                new SimpleSerializer());

        replayerGateway.await();
        GraphGateway.awaitShutdown();
    }

    private static void addNode(int i, Simulator simulator, Random rand, Instant time) {
        String id = Integer.toString(i);

        simulator.addCoroutineActor(
                id,
                new UnstructuredClientCoroutine(),
                Duration.ZERO,
                time,
                new Start(
                        new SimpleAddressTransformer(Address.of(), id),
                        Address.of("" + rand.nextInt(i)),
                        (long) i,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }

    private static void addSeedNode(Simulator simulator, Instant time) {
        simulator.addCoroutineActor(
                "0",
                new UnstructuredClientCoroutine(),
                Duration.ZERO,
                time,
                new Start(
                        new SimpleAddressTransformer(Address.of(), "0"),
                        0L,
                        Address.of("timer"),
                        Address.of("graph"),
                        Address.of("log")
                )
        );
    }
}
