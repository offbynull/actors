package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.network.NetworkEndpointFinder;
import com.offbynull.peernetic.actor.network.transports.test.PerfectLine;
import com.offbynull.peernetic.actor.network.transports.test.TestHub;
import com.offbynull.peernetic.actor.network.transports.test.TestTransport;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.math.BigInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author User
 */
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
        
        
        
        TestTransport<Integer> transport0 = new TestTransport<>(0, hubRunner.getEndpoint());
        ActorRunner transport0Runner = ActorRunner.createAndStart(transport0);
        NetworkEndpointFinder<Integer> finder0 = new NetworkEndpointFinder<>(transport0Runner.getEndpoint());
        
        Id id0 = new Id(BigInteger.ZERO.toByteArray(), new BigInteger("7").toByteArray());
        Integer address0 = 0;
        Pointer<Integer> ptr0 = new Pointer<>(id0, address0);
        ChordOverlay<Integer> overlay0 = new ChordOverlay<>(ptr0, null, finder0);
        ActorRunner overlay0Runner = ActorRunner.createAndStart(overlay0);
        
        transport0.setDestinationEndpoint(overlay0Runner.getEndpoint());
        
        
        
        TestTransport<Integer> transport4 = new TestTransport<>(4, hubRunner.getEndpoint());
        ActorRunner transport4Runner = ActorRunner.createAndStart(transport4);
        NetworkEndpointFinder<Integer> finder4 = new NetworkEndpointFinder<>(transport4Runner.getEndpoint());
        
        Id id4 = new Id(new BigInteger("4").toByteArray(), new BigInteger("7").toByteArray());
        Integer address4 = 0;
        Pointer<Integer> ptr4 = new Pointer<>(id4, address4);
        ChordOverlay<Integer> overlay4 = new ChordOverlay<>(ptr4, ptr0, finder4);
        ActorRunner overlay4Runner = ActorRunner.createAndStart(overlay4);
        
        transport4.setDestinationEndpoint(overlay4Runner.getEndpoint());
        
        
        
        Thread.sleep(1000000L);
    }
}
