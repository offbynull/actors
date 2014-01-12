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
        
        
        
        Thread.sleep(15000L);
        
        
        
        overlay1Runner.stop();
        overlay2Runner.stop();
        overlay3Runner.stop();
        
        
        Thread.sleep(100000L);
    }
    
    private ActorRunner generateNode(int id, int limit, Integer bootstrap, ChordOverlayListener<Integer> listener, ActorRunner hubRunner) {
        TestTransport<Integer> transport = new TestTransport<>(id, hubRunner.getEndpoint());
        ActorRunner transportRunner = ActorRunner.createAndStart(transport);
        NetworkEndpointFinder<Integer> finder = new NetworkEndpointFinder<>(transportRunner.getEndpoint());
        
        Id selfId = new Id(new BigInteger("" + id).toByteArray(), new BigInteger("" + limit).toByteArray());
        Pointer<Integer> selfPtr = new Pointer<>(selfId, id);
        
        Pointer<Integer> bootstrapPtr = null;
        if (bootstrap != null) {
            Id bootstrapId = new Id(new BigInteger("" + bootstrap).toByteArray(), new BigInteger("" + limit).toByteArray());
            bootstrapPtr = new Pointer<>(bootstrapId, bootstrap);
        }
        
        ChordOverlay<Integer> overlay = new ChordOverlay<>(selfPtr, bootstrapPtr, finder, listener);
        ActorRunner overlayRunner = ActorRunner.createAndStart(overlay);
        
        transport.setDestinationEndpoint(overlayRunner.getEndpoint());
        
        return overlayRunner;
    }
}
