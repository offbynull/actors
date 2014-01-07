package com.offbynull.peernetic.rpc.transport.transports.tcp;

import com.offbynull.peernetic.rpc.transport.transports.shared.RequestActor;
import com.offbynull.peernetic.rpc.transport.transports.shared.ResponseActor;
import com.offbynull.peernetic.actor.ActorRunner;
import com.offbynull.peernetic.rpc.transport.NetworkEndpoint;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TcpActorTest {
    
    public TcpActorTest() {
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
        InetSocketAddress requestAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 9001);
        InetSocketAddress responseAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 9002);
        
        // start responder
        ResponseActor responder = new ResponseActor();
        ActorRunner respondRunner = ActorRunner.createAndStart(responder);
        
        TcpTransport responderTransport = new TcpTransport(responseAddress, 65535, 10000L);
        responderTransport.setDestinationEndpoint(respondRunner.getEndpoint());
        ActorRunner respondTransportRunner = ActorRunner.createAndStart(responderTransport);
        
        
        // start requester
        RequestActor requestActor = new RequestActor();
        ActorRunner requestRunner = ActorRunner.createAndStart(requestActor);
        
        TcpTransport requesterTransport = new TcpTransport(requestAddress, 65535, 10000L);
        requesterTransport.setDestinationEndpoint(requestRunner.getEndpoint());
        ActorRunner requestTransportRunner = ActorRunner.createAndStart(requesterTransport);
        
        NetworkEndpoint<InetSocketAddress> endpointToResponder = new NetworkEndpoint<>(requestTransportRunner.getEndpoint(), responseAddress);
        
        
        requestActor.beginRequests(requestRunner.getEndpoint(), endpointToResponder); // need to do this to get the ball rolling, because
                                                                                      // currently cyclical endpoints are difficult to do :(
        

        
        
        
        Thread.sleep(6000L);
        
        Assert.assertEquals(5L, requestActor.getNumber());
        
        requestRunner.stop();
        respondRunner.stop();
        requestTransportRunner.stop();
        respondTransportRunner.stop();
    }
}
