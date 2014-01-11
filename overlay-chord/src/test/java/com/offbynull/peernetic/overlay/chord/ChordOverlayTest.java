package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.SimpleEndpointFinder;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
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
    public void hello() throws Throwable {
        Map<Integer, Endpoint> finderMap = new HashMap<>();
        finderMap.put(0, new NullEndpoint());
        finderMap.put(1, new NullEndpoint());
        finderMap.put(2, new NullEndpoint());
        finderMap.put(3, new NullEndpoint());
        finderMap.put(4, new NullEndpoint());
        finderMap.put(5, new NullEndpoint());
        finderMap.put(6, new NullEndpoint());
        finderMap.put(7, new NullEndpoint());
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(finderMap);
        
        Id id = new Id(BigInteger.ZERO.toByteArray(), new BigInteger("7").toByteArray());
        Integer address = 0;
        
        Pointer<Integer> self = new Pointer<>(id, address);
        ChordOverlay<Integer> chordOverlay = new ChordOverlay(self, null, finder);
        
        ActorRunner runner = ActorRunner.createAndStart(chordOverlay);
        
        Thread.sleep(100000L);
    }
}
