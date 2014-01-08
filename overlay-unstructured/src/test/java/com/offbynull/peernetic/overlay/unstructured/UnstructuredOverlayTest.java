package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.actor.ActorTester;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.EndpointKeyExtractor;
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
    public void startUpTest() throws Throwable {
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
        endpointMap.put(10, new NullEndpoint());
        
        Set<Integer> cache = new HashSet<>(endpointMap.keySet());
        EndpointFinder<Integer> finder = new SimpleEndpointFinder<>(endpointMap);
        EndpointKeyExtractor<Integer> extractor = new SimpleEndpointKeyExtractor<>(endpointMap.inverseBidiMap());
        
        
        
        UnstructuredOverlay<Integer> overlay = new UnstructuredOverlay(listenerMock, finder, extractor, 10, 100L, cache);
        ActorTester tester = new ActorTester(overlay);
        
        Collection<Outgoing> outgoing;
        
        outgoing = tester.start(0L);
        Assert.assertEquals(Collections.emptyList(), outgoing);
    }
}
