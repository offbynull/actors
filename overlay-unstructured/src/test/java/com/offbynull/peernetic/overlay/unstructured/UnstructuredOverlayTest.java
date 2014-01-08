package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.actor.ActorTester;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.EndpointKeyExtractor;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.Outgoing;
import com.offbynull.peernetic.actor.SimpleEndpointFinder;
import com.offbynull.peernetic.actor.SimpleEndpointKeyExtractor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class UnstructuredOverlayTest {

    public UnstructuredOverlayTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Throwable {
    }

    @After
    public void tearDown() throws Throwable {
    }

    @Test
    public void outgoingFailedHandshakeTest() throws Throwable {
        // setup
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);
        
        DualHashBidiMap<Integer, Endpoint> endpointMap = new DualHashBidiMap<>();
        endpointMap.put(1, new NullEndpoint());
        endpointMap.put(2, new NullEndpoint());
        endpointMap.put(3, new NullEndpoint());
        endpointMap.put(4, new NullEndpoint());
        endpointMap.put(5, new NullEndpoint());
        endpointMap.put(6, new NullEndpoint());
        endpointMap.put(7, new NullEndpoint());
        endpointMap.put(8, new NullEndpoint());
        endpointMap.put(9, new NullEndpoint());
        
        Set<Integer> cache = new HashSet<>(endpointMap.keySet());
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());

        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;

        
        // start up
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately and make sure 5 new connection requests are made
        outgoing = tester.step(0L);
        Assert.assertEquals(5, outgoing.size());
        for (Outgoing outgoingMsg : outgoing) {
            Assert.assertEquals(InitiateJoinCommand.class, outgoingMsg.getContent().getClass());
        }
        
        // step again at 25ms and make sure no new connection requests are made
        outgoing = tester.step(25L);
        Assert.assertEquals(Collections.emptyList(), outgoing);

        // step again at 50ms and make sure no new connection requests are made
        outgoing = tester.step(50L);
        Assert.assertEquals(Collections.emptyList(), outgoing);

        // step again at 75ms and make sure no new connection requests are made
        outgoing = tester.step(75L);
        Assert.assertEquals(Collections.emptyList(), outgoing);

        // step again at 100ms and make sure 4 new connection requests are made
        outgoing = tester.step(100L);
        Assert.assertEquals(4, outgoing.size());
        for (Outgoing outgoingMsg : outgoing) {
            Assert.assertEquals(InitiateJoinCommand.class, outgoingMsg.getContent().getClass());
        }
        
        // step again at 210ms and make sure no new connection requests are made
        outgoing = tester.step(210L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
    }


    @Test
    public void outgoingGoodHandshakeRequestThenNoKeepaliveTest() throws Throwable {
        // setup
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);
        
        DualHashBidiMap<Integer, Endpoint> endpointMap = new DualHashBidiMap<>();
        endpointMap.put(1, new NullEndpoint());
        
        Set<Integer> cache = new HashSet<>(endpointMap.keySet());
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());

        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;
        Outgoing outgoingMsg;

        
        // start up
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately and get first connection request, and step again with an okay reply
        outgoing = tester.step(0L);
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(InitiateJoinCommand.class, outgoingMsg.getContent().getClass());
        
        Endpoint fromEndpoint = outgoingMsg.getDestination();
        Integer fromAddress = endpointMap.getKey(fromEndpoint);
        Object okayResponse = new JoinSuccessfulCommand(new State(Collections.emptySet(), Collections.emptySet())); 
        tester.step(0L, new Incoming(okayResponse, fromEndpoint));
        
        Mockito.verify(listenerMock).linkCreated(overlay, LinkType.OUTGOING, fromAddress);
        
        // step at 40ms and make sure nothing comes in
        outgoing = tester.step(40L);
        Assert.assertEquals(Collections.emptyList(), outgoing);

        // step at 50ms and make sure keepalive comes in
        outgoing = tester.step(50L);
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(InitiateKeepAliveCommand.class, outgoingMsg.getContent().getClass());
        
        // step at 100ms and make sure disconnect
        outgoing = tester.step(100L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        Mockito.verify(listenerMock).linkDestroyed(overlay, LinkType.OUTGOING, fromAddress);
    }

    @Test
    public void outgoingGoodHandshakeRequestThenGoodKeepaliveTest() throws Throwable {
        // setup
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);
        
        DualHashBidiMap<Integer, Endpoint> endpointMap = new DualHashBidiMap<>();
        endpointMap.put(1, new NullEndpoint());
        
        Set<Integer> cache = new HashSet<>(endpointMap.keySet());
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());

        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;
        Outgoing outgoingMsg;
        Endpoint fromEndpoint;
        Integer fromAddress;

        
        // start up
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately and get first connection request, and step again with an okay reply
        outgoing = tester.step(0L);
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(InitiateJoinCommand.class, outgoingMsg.getContent().getClass());
        
        fromEndpoint = outgoingMsg.getDestination();
        fromAddress = endpointMap.getKey(fromEndpoint);
        Object okayJoinResponse = new JoinSuccessfulCommand(new State(Collections.emptySet(), Collections.emptySet())); 
        tester.step(0L, new Incoming(okayJoinResponse, fromEndpoint));
        
        Mockito.verify(listenerMock).linkCreated(overlay, LinkType.OUTGOING, fromAddress);
        
        // step at 40ms and make sure nothing comes in
        outgoing = tester.step(40L);
        Assert.assertEquals(Collections.emptyList(), outgoing);

        // step at 50ms and make sure keepalive comes in
        outgoing = tester.step(50L);
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(InitiateKeepAliveCommand.class, outgoingMsg.getContent().getClass());

        fromEndpoint = outgoingMsg.getDestination();
        fromAddress = endpointMap.getKey(fromEndpoint);
        Object okayKeepAliveResponse = new KeepAliveSuccessfulCommand(new State(Collections.emptySet(), Collections.emptySet())); 
        tester.step(50L, new Incoming(okayKeepAliveResponse, fromEndpoint));

        // step at 100ms and make sure another keepalive and no disconnect
        outgoing = tester.step(100L);
        outgoingMsg = outgoing.iterator().next();

        Assert.assertEquals(InitiateKeepAliveCommand.class, outgoingMsg.getContent().getClass());
        
        Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(overlay, LinkType.OUTGOING, fromAddress);
    }

    @Test
    public void outgoingGoodHandshakeRequestThenFailedKeepaliveTest() throws Throwable {
        // setup
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);
        
        DualHashBidiMap<Integer, Endpoint> endpointMap = new DualHashBidiMap<>();
        endpointMap.put(1, new NullEndpoint());
        
        Set<Integer> cache = new HashSet<>(endpointMap.keySet());
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());

        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;
        Outgoing outgoingMsg;
        Endpoint fromEndpoint;
        Integer fromAddress;

        
        // start up
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately and get first connection request, and step again with an okay reply
        outgoing = tester.step(0L);
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(InitiateJoinCommand.class, outgoingMsg.getContent().getClass());
        
        fromEndpoint = outgoingMsg.getDestination();
        fromAddress = endpointMap.getKey(fromEndpoint);
        Object okayJoinResponse = new JoinSuccessfulCommand(new State(Collections.emptySet(), Collections.emptySet())); 
        tester.step(0L, new Incoming(okayJoinResponse, fromEndpoint));
        
        Mockito.verify(listenerMock).linkCreated(overlay, LinkType.OUTGOING, fromAddress);
        
        // step at 40ms and make sure nothing comes in
        outgoing = tester.step(40L);
        Assert.assertEquals(Collections.emptyList(), outgoing);

        // step at 50ms and make sure keepalive comes in
        outgoing = tester.step(50L);
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(InitiateKeepAliveCommand.class, outgoingMsg.getContent().getClass());

        fromEndpoint = outgoingMsg.getDestination();
        fromAddress = endpointMap.getKey(fromEndpoint);
        Object failedKeepAliveResponse = new KeepAliveFailedCommand(new State(Collections.emptySet(), Collections.emptySet())); 
        tester.step(50L, new Incoming(failedKeepAliveResponse, fromEndpoint));

        // step at 50ms again and make sure disconnect
        outgoing = tester.step(50L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        Mockito.verify(listenerMock).linkDestroyed(overlay, LinkType.OUTGOING, fromAddress);
    }
    
    @Test
    public void incomingGoodHandshakeRequestThenGoodKeepaliveThenNoKeepAliveTest() throws Throwable {
        // setup
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);
        
        Endpoint fakeEndpoint = new NullEndpoint();
        DualHashBidiMap<Integer, Endpoint> endpointMap = new DualHashBidiMap<>();
        endpointMap.put(1, fakeEndpoint);
        
        Set<Integer> cache = Collections.emptySet();
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());

        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;
        Outgoing outgoingMsg;
        Endpoint fromEndpoint;

        
        // start up
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately and make sure no outgoing requests
        outgoing = tester.step(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately with a new connection request and make sure a successful join msg comes back
        fromEndpoint = fakeEndpoint;
        Object initiateJoinRequest = new InitiateJoinCommand();
        
        outgoing = tester.step(0L, new Incoming(initiateJoinRequest, fromEndpoint));
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(JoinSuccessfulCommand.class, outgoingMsg.getContent().getClass());
        
        Mockito.verify(listenerMock).linkCreated(overlay, LinkType.INCOMING, 1);
        

        // step at 50ms and make sure you get back nothing
        outgoing = tester.step(50L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step at 60ms with a keepalive command and make sure keepalive successful
        fromEndpoint = fakeEndpoint;
        Object initiateKeepAliveRequest = new InitiateKeepAliveCommand();
        
        outgoing = tester.step(0L, new Incoming(initiateKeepAliveRequest, fromEndpoint));
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(KeepAliveSuccessfulCommand.class, outgoingMsg.getContent().getClass());
        
        // step at 160ms and make sure link destroyed
        outgoing = tester.step(160L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        Mockito.verify(listenerMock).linkDestroyed(overlay, LinkType.INCOMING, 1);
    }
    
    @Test
    public void incomingGoodHandshakeDuplicateRequestTest() throws Throwable {
        // setup
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);
        
        Endpoint fakeEndpoint = new NullEndpoint();
        DualHashBidiMap<Integer, Endpoint> endpointMap = new DualHashBidiMap<>();
        endpointMap.put(1, fakeEndpoint);
        
        Set<Integer> cache = Collections.emptySet();
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());

        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;
        Outgoing outgoingMsg;
        Endpoint fromEndpoint;

        
        // start up
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately and make sure no outgoing requests
        outgoing = tester.step(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately with a new connection request and make sure a successful join msg comes back
        fromEndpoint = fakeEndpoint;
        Object initiateJoinRequest = new InitiateJoinCommand();
        
        outgoing = tester.step(0L, new Incoming(initiateJoinRequest, fromEndpoint));
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(JoinSuccessfulCommand.class, outgoingMsg.getContent().getClass());
        
        Mockito.verify(listenerMock).linkCreated(overlay, LinkType.INCOMING, 1);
        
        // step at 1ms with a new connection request and make sure a successful join msg comes back but no trigger of linkCreated
        fromEndpoint = fakeEndpoint;
        Object initiateJoinRequest2 = new InitiateJoinCommand();
        
        outgoing = tester.step(1L, new Incoming(initiateJoinRequest2, fromEndpoint));
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(JoinSuccessfulCommand.class, outgoingMsg.getContent().getClass());
        
        Mockito.verify(listenerMock).linkCreated(overlay, LinkType.INCOMING, 1); // make sure it was still only called once
    }
    
    @Test
    public void incomingNoHandshakeRequestThenKeepaliveTest() throws Throwable {
        // setup
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);
        
        Endpoint fakeEndpoint = new NullEndpoint();
        DualHashBidiMap<Integer, Endpoint> endpointMap = new DualHashBidiMap<>();
        endpointMap.put(1, fakeEndpoint);
        
        Set<Integer> cache = Collections.emptySet();
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());

        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;
        Outgoing outgoingMsg;
        Endpoint fromEndpoint;

        
        // start up
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step immediately and make sure no outgoing requests
        outgoing = tester.step(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
        
        // step at 60ms with a keepalive command and make sure keepalive failed
        fromEndpoint = fakeEndpoint;
        Object initiateKeepAliveRequest = new InitiateKeepAliveCommand();
        
        outgoing = tester.step(60L, new Incoming(initiateKeepAliveRequest, fromEndpoint));
        outgoingMsg = outgoing.iterator().next();
        
        Assert.assertEquals(KeepAliveFailedCommand.class, outgoingMsg.getContent().getClass());
        
        Mockito.verify(listenerMock).linkDestroyed(overlay, LinkType.INCOMING, 1);
    }
}
