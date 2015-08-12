package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.actor.ActorRunner;
import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.peernetic.network.actors.udpsimulator.SimpleLine;
import com.offbynull.peernetic.network.actors.udpsimulator.StartUdpSimulator;
import com.offbynull.peernetic.network.actors.udpsimulator.UdpSimulatorCoroutine;
import com.offbynull.peernetic.visualizer.gateways.graph.DefaultGraphNodeRemoveHandler;
import java.time.Duration;
import java.util.Random;

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
    
    private static final int MAX_NODES = 100;
    private static final int MAX_WAIT_PER_NODE_ADD = 1000; // in milliseconds
    private static final int MAX_GRAPH_X = 1000;
    private static final int MAX_GRAPH_Y = 1000;
    
    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();

        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);
        TimerGateway timerGateway = new TimerGateway(BASE_TIMER_ADDRESS_STRING);
        LogGateway logGateway = new LogGateway(BASE_LOG_ADDRESS_STRING);
        ActorRunner actorRunner = new ActorRunner(BASE_ACTOR_ADDRESS_STRING);

        timerGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(graphGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());
        
        graphGateway.setHandlers(new CustomGraphNodeAddHandler(MAX_GRAPH_X, MAX_GRAPH_Y), new DefaultGraphNodeRemoveHandler());

        Random rand = new Random(12345);

        // Seed node
        addUdpSimulatorProxy(0, actorRunner, 0);
        addNode(0, null, actorRunner);
        
        // Connecting nodes
        for (int i = 1; i < MAX_NODES; i++) {
            addUdpSimulatorProxy(i, actorRunner, i);
            addNode(i, 0, actorRunner);
            Thread.sleep(rand.nextInt(MAX_WAIT_PER_NODE_ADD));
        }

        GraphGateway.awaitShutdown();
    }

    private static void addUdpSimulatorProxy(int id, ActorRunner actorRunner, int randomSeed) {
        String idStr = Integer.toString(id);
        String unreliableIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        
        actorRunner.addActor(
                unreliableIdStr,
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
    
    private static void addNode(int id, Integer connId, ActorRunner actorRunner) {
        String idStr = Integer.toString(id);
        String udpSimProxyIdStr = String.format(SIMULATED_UDP_PROXY_ID_FORMAT, id);
        Address remoteBaseAddr = BASE_ACTOR_ADDRESS
                    .appendSuffix(udpSimProxyIdStr)
                    .appendSuffix(BASE_ACTOR_ADDRESS);
        String connIdStr = connId == null ? null : String.format(SIMULATED_UDP_PROXY_ID_FORMAT, connId);

        byte[] seed = new byte[MIN_SEED_SIZE];
        seed[0] = (byte) id;

        actorRunner.addActor(
                idStr,
                new UnstructuredClientCoroutine(),
                new Start(
                        new SimpleAddressTransformer(remoteBaseAddr, udpSimProxyIdStr),
                        connIdStr,                                                 
                        seed,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
}
