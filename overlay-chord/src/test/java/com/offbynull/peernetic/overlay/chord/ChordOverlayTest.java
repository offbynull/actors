package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.network.IncomingMessageToEndpointAdapter;
import com.offbynull.peernetic.actor.network.NetworkEndpointFinder;
import com.offbynull.peernetic.networktools.netty.builders.NettyClient;
import com.offbynull.peernetic.networktools.netty.builders.NettyClientBuilder;
import com.offbynull.peernetic.networktools.netty.simulation.PerfectLine;
import com.offbynull.peernetic.networktools.netty.simulation.TransitPacketRepository;
import com.offbynull.peernetic.networktools.netty.simulation.WrappedSocketAddress;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.math.BigInteger;
import java.net.SocketAddress;
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
        TransitPacketRepository hub = TransitPacketRepository.create(new PerfectLine());
        
        ChordOverlayListener<SocketAddress> listener0 = Mockito.mock(ChordOverlayListener.class);
        ChordOverlayListener<SocketAddress> listener1 = Mockito.mock(ChordOverlayListener.class);
        ChordOverlayListener<SocketAddress> listener2 = Mockito.mock(ChordOverlayListener.class);
        ChordOverlayListener<SocketAddress> listener3 = Mockito.mock(ChordOverlayListener.class);
        
        
        ActorRunner overlay0Runner = generateNode(0, null, listener0, hub);
        ActorRunner overlay1Runner = generateNode(1, 0, listener1, hub);
        ActorRunner overlay2Runner = generateNode(2, 0, listener2, hub);
        ActorRunner overlay3Runner = generateNode(3, 0, listener3, hub);
        
        
        
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
    
    private List<Pointer<SocketAddress>> generatePointerList(int ... ids) {
        List<Pointer<SocketAddress>> pointers = new ArrayList<>();
        
        for (int id : ids) {
            pointers.add(generatePointer(id));
        }
        
        return pointers;
    }
    private Pointer<SocketAddress> generatePointer(int id) {
        Id selfId = new Id(new BigInteger("" + id).toByteArray(), new BigInteger("3").toByteArray());
        return new Pointer<>(selfId, (SocketAddress) new WrappedSocketAddress(id));
    }
    
    private ActorRunner generateNode(int id, Integer bootstrap, ChordOverlayListener<SocketAddress> listener,
            TransitPacketRepository packetRepository) throws Throwable {
        SocketAddress selfAddress = new WrappedSocketAddress(id);
        SocketAddress bootstrapAddress = bootstrap == null ? null : new WrappedSocketAddress(bootstrap);
        
        IncomingMessageToEndpointAdapter msgToEndpointAdapter = new IncomingMessageToEndpointAdapter();
        NettyClient nettyClient = new NettyClientBuilder()
                .simulatedUdp(selfAddress, packetRepository)
                .addReadDestination(msgToEndpointAdapter)
                .build();
        
        NetworkEndpointFinder finder = new NetworkEndpointFinder(nettyClient.channel());
        ChordConfig<SocketAddress> config = new ChordConfig<>(selfAddress, new BigInteger("3").toByteArray(), finder);
        
        config.setListener(listener);
        config.setRpcMaxSendAttempts(3);
        config.setRpcTimeoutDuration(200L);
        config.setStabilizePeriod(500L);
        config.setFixFingerPeriod(500L);
        
        
        Id selfId = new Id(new BigInteger("" + id).toByteArray(), new BigInteger("3").toByteArray());
        Pointer<SocketAddress> selfPtr = new Pointer<>(selfId, selfAddress);
        config.setBase(selfPtr);
        
        if (bootstrap != null) {
            Id bootstrapId = new Id(new BigInteger("" + bootstrap).toByteArray(), new BigInteger("3").toByteArray());
            Pointer<SocketAddress> bootstrapPtr = new Pointer<>(bootstrapId, bootstrapAddress);
            config.setBootstrap(bootstrapPtr);
        }
        
        ChordOverlay<SocketAddress> overlay = new ChordOverlay<>(config);
        ActorRunner overlayRunner = ActorRunner.createAndStart(overlay);
        
        msgToEndpointAdapter.setEndpoint(overlayRunner.getEndpoint());
        
        
        return overlayRunner;
    }
}
