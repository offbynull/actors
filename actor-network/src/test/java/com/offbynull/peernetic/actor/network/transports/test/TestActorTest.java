package com.offbynull.peernetic.actor.network.transports.test;

import com.offbynull.peernetic.actor.network.transports.test.PerfectLine;
import com.offbynull.peernetic.actor.network.transports.test.TestTransport;
import com.offbynull.peernetic.actor.network.transports.test.TestHub;
import com.offbynull.peernetic.actor.network.transports.shared.RequestActor;
import com.offbynull.peernetic.actor.network.transports.shared.ResponseActor;
import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.actor.network.NetworkEndpoint;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestActorTest {
    
    public TestActorTest() {
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
    public void basicActorTest() throws Throwable {
        // start hub
        TestHub<String> hubActor = new TestHub<>(new PerfectLine<String>());
        ActorRunner hubRunner = ActorRunner.createAndStart(hubActor);
        
        
        // start responder
        ResponseActor responder = new ResponseActor();
        ActorRunner respondRunner = ActorRunner.createAndStart(responder);
        
        TestTransport<String> responderTransport = new TestTransport("responder", hubRunner.getEndpoint());
        responderTransport.setDestinationEndpoint(respondRunner.getEndpoint());
        ActorRunner respondTransportRunner = ActorRunner.createAndStart(responderTransport);
        
        
        // start requester
        RequestActor requestActor = new RequestActor();
        ActorRunner requestRunner = ActorRunner.createAndStart(requestActor);
        
        TestTransport<String> requesterTransport = new TestTransport("requester", hubRunner.getEndpoint());
        requesterTransport.setDestinationEndpoint(requestRunner.getEndpoint());
        ActorRunner requestTransportRunner = ActorRunner.createAndStart(requesterTransport);
        
        NetworkEndpoint<String> endpointToResponder = new NetworkEndpoint<>(requestTransportRunner.getEndpoint(), "responder");
        
        
        requestActor.beginRequests(requestRunner.getEndpoint(), endpointToResponder); // need to do this to get the ball rolling, because
                                                                                      // currently cyclical endpoints are difficult to do :(
        

        
        
        
        Thread.sleep(6000L);
        
        Assert.assertEquals(5L, requestActor.getNumber());
        
        requestRunner.stop();
        respondRunner.stop();
        requestTransportRunner.stop();
        respondTransportRunner.stop();
        hubRunner.stop();
    }
}
