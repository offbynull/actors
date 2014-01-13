package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.network.NetworkEndpointFinder;
import com.offbynull.peernetic.actor.network.transports.test.PerfectLine;
import com.offbynull.peernetic.actor.network.transports.test.TestHub;
import com.offbynull.peernetic.actor.network.transports.test.TestTransport;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

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
        
        ChordOverlayListener<Integer> listener0 = Mockito.mock(ChordOverlayListener.class);
        ChordOverlayListener<Integer> listener1 = Mockito.mock(ChordOverlayListener.class);
        ChordOverlayListener<Integer> listener2 = Mockito.mock(ChordOverlayListener.class);
        ChordOverlayListener<Integer> listener3 = Mockito.mock(ChordOverlayListener.class);
        
        
        ActorRunner overlay0Runner = generateNode(0, null, listener0, hubRunner);
        ActorRunner overlay1Runner = generateNode(1, 0, listener1, hubRunner);
        ActorRunner overlay2Runner = generateNode(2, 0, listener2, hubRunner);
        ActorRunner overlay3Runner = generateNode(3, 0, listener3, hubRunner);
        
        
        
        Thread.sleep(10000L);
        
        Mockito.verify(listener0, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(0)), // self
                Mockito.eq(generatePointer(3)), // pred
                Mockito.eq(generatePointerList(1, 2)), // fingers
                Mockito.eq(generatePointerList(1, 2))); // successors
        Mockito.verify(listener1, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(1)), // self
                Mockito.eq(generatePointer(0)), // pred
                Mockito.eq(generatePointerList(2, 3)), // fingers
                Mockito.eq(generatePointerList(2, 3))); // successors
        Mockito.verify(listener2, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(2)), // self
                Mockito.eq(generatePointer(1)), // pred
                Mockito.eq(generatePointerList(3, 0)), // fingers
                Mockito.eq(generatePointerList(3, 0))); // successors
        Mockito.verify(listener3, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(3)), // self
                Mockito.eq(generatePointer(2)), // pred
                Mockito.eq(generatePointerList(0, 1)), // fingers
                Mockito.eq(generatePointerList(0, 1))); // successors
        
        Mockito.reset(listener0);
        Mockito.reset(listener1);
        Mockito.reset(listener2);
        Mockito.reset(listener3);
        
        
        
        System.out.println("REMOVING 1");
        overlay1Runner.stop();
        Thread.sleep(6000L);

        Mockito.verify(listener0, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(0)), // self
                Mockito.eq(generatePointer(3)), // pred
                Mockito.eq(generatePointerList(2, 2)), // fingers
                Mockito.eq(generatePointerList(2, 3))); // successors
        Mockito.verify(listener2, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(2)), // self
                Mockito.eq(generatePointer(0)), // pred
                Mockito.eq(generatePointerList(3, 0)), // fingers
                Mockito.eq(generatePointerList(3, 0))); // successors
        Mockito.verify(listener3, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(3)), // self
                Mockito.eq(generatePointer(2)), // pred
                Mockito.eq(generatePointerList(0, 2)), // fingers
                Mockito.eq(generatePointerList(0, 2))); // successors

        Mockito.reset(listener0);
        Mockito.reset(listener1);
        Mockito.reset(listener2);
        Mockito.reset(listener3);
        
        
        
        System.out.println("REMOVING 2");
        overlay2Runner.stop();
        Thread.sleep(6000L);
        
        Mockito.verify(listener0, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(0)), // self
                Mockito.eq(generatePointer(3)), // pred
                Mockito.eq(generatePointerList(3, 3)), // fingers
                Mockito.eq(generatePointerList(3))); // successors
        Mockito.verify(listener3, Mockito.atLeastOnce()).stateUpdated(
                Mockito.anyString(),
                Mockito.eq(generatePointer(3)), // self
                Mockito.eq(generatePointer(0)), // pred
                Mockito.eq(generatePointerList(0, 3)), // fingers
                Mockito.eq(generatePointerList(0))); // successors
    }
    
    private List<Pointer<Integer>> generatePointerList(int ... ids) {
        List<Pointer<Integer>> pointers = new ArrayList<>();
        
        for (int id : ids) {
            pointers.add(generatePointer(id));
        }
        
        return pointers;
    }
    private Pointer<Integer> generatePointer(int id) {
        Id selfId = new Id(new BigInteger("" + id).toByteArray(), new BigInteger("3").toByteArray());
        return new Pointer<>(selfId, id);
    }
    
    private ActorRunner generateNode(int id, Integer bootstrap, ChordOverlayListener<Integer> listener, ActorRunner hubRunner)
            throws Throwable {
        TestTransport<Integer> transport = new TestTransport<>(id, hubRunner.getEndpoint());
        ActorRunner transportRunner = ActorRunner.createAndStart(transport);
        
        
        NetworkEndpointFinder<Integer> finder = new NetworkEndpointFinder<>(transportRunner.getEndpoint());
        ChordConfig<Integer> config = new ChordConfig<>(id, new BigInteger("3").toByteArray(), finder);
        
        config.setListener(listener);
        config.setRpcMaxSendAttempts(3);
        config.setRpcTimeoutDuration(200L);
        config.setStabilizePeriod(500L);
        config.setFixFingerPeriod(500L);
        
        
        Id selfId = new Id(new BigInteger("" + id).toByteArray(), new BigInteger("3").toByteArray());
        Pointer<Integer> selfPtr = new Pointer<>(selfId, id);
        config.setBase(selfPtr);
        
        if (bootstrap != null) {
            Id bootstrapId = new Id(new BigInteger("" + bootstrap).toByteArray(), new BigInteger("3").toByteArray());
            Pointer<Integer> bootstrapPtr = new Pointer<>(bootstrapId, bootstrap);
            config.setBootstrap(bootstrapPtr);
        }
        
        ChordOverlay<Integer> overlay = new ChordOverlay<>(config);
        ActorRunner overlayRunner = ActorRunner.createAndStart(overlay);
        
        transport.setDestinationEndpoint(overlayRunner.getEndpoint());
        
        
        return overlayRunner;
    }
}
