package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.network.NetworkEndpointFinder;
import com.offbynull.peernetic.actor.network.transports.test.PerfectLine;
import com.offbynull.peernetic.actor.network.transports.test.TestHub;
import com.offbynull.peernetic.actor.network.transports.test.TestTransport;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.math.BigInteger;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChordOverlayTest {
    
    public ChordOverlayTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void simpleJoinTest() throws Throwable {
        TestHub<Integer> hub = new TestHub<>(new PerfectLine<Integer>());
        ActorRunner hubRunner = ActorRunner.createAndStart(hub);
        
        ChordOverlayListener<Integer> listener = new ChordOverlayListener<Integer>() {

            @Override
            public void stateUpdated(String event, Pointer<Integer> self, Pointer<Integer> predecessor, List<Pointer<Integer>> fingerTable, List<Pointer<Integer>> successorTable) {
                System.out.println(event + " - " +self.getId());
                System.out.println("Fingers: " + fingerTable);
                System.out.println("Successors: " + successorTable);
                System.out.println("Predecessors: " + predecessor);
                System.out.println("----------------------------");
            }

            @Override
            public void failed(FailureMode failureMode) {
                System.out.println(failureMode);
            }
        };
        
        
        ActorRunner overlay0Runner = generateNode(0, 3, null, listener, hubRunner);
        ActorRunner overlay1Runner = generateNode(1, 3, 0, listener, hubRunner);
        ActorRunner overlay2Runner = generateNode(2, 3, 0, listener, hubRunner);
        ActorRunner overlay3Runner = generateNode(3, 3, 0, listener, hubRunner);
        
        
        
        Thread.sleep(6000L);
        System.out.println("REMOVING 1");
        overlay1Runner.stop();
        
        Thread.sleep(6000L);
        System.out.println("REMOVING 2");
        overlay2Runner.stop();

//        
//        Thread.sleep(1500000L);
//        System.out.println("REMOVING 2");
//        overlay2Runner.stop();
//        
        
        Thread.sleep(1000000L);
    }
    
    private ActorRunner generateNode(int id, int limit, Integer bootstrap, ChordOverlayListener<Integer> listener, ActorRunner hubRunner)
            throws Throwable {
        TestTransport<Integer> transport = new TestTransport<>(id, hubRunner.getEndpoint());
        ActorRunner transportRunner = ActorRunner.createAndStart(transport);
        
        
        NetworkEndpointFinder<Integer> finder = new NetworkEndpointFinder<>(transportRunner.getEndpoint());
        ChordConfig<Integer> config = new ChordConfig<>(id, new BigInteger("" + limit).toByteArray(), finder);
        
        config.setListener(listener);
        config.setRpcMaxSendAttempts(3);
        config.setRpcTimeoutDuration(200L);
        config.setStabilizePeriod(500L);
        config.setFixFingerPeriod(500L);
        
        
        Id selfId = new Id(new BigInteger("" + id).toByteArray(), new BigInteger("" + limit).toByteArray());
        Pointer<Integer> selfPtr = new Pointer<>(selfId, id);
        config.setBase(selfPtr);
        
        if (bootstrap != null) {
            Id bootstrapId = new Id(new BigInteger("" + bootstrap).toByteArray(), new BigInteger("" + limit).toByteArray());
            Pointer<Integer> bootstrapPtr = new Pointer<>(bootstrapId, bootstrap);
            config.setBootstrap(bootstrapPtr);
        }
        
        ChordOverlay<Integer> overlay = new ChordOverlay<>(config);
        ActorRunner overlayRunner = ActorRunner.createAndStart(overlay);
        
        transport.setDestinationEndpoint(overlayRunner.getEndpoint());
        
        
        return overlayRunner;
    }
}
