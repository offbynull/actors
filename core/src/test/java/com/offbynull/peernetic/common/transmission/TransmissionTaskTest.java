package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.SimpleEndpointScheduler;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public final class TransmissionTaskTest {

    private Endpoint userEndpoint;
    private Endpoint transEndpoint;
    private Endpoint networkNode1Endpoint;
    private Endpoint networkNode2Endpoint;
    private ActorRunnable actorRunnable;
    private EndpointDirectory networkEndpointDirectory;
    private EndpointIdentifier networkEndpointIdentifier;
    private TransmissionEndpointDirectory<Integer> transmissionEndpointDirectory;
    private TransmissionEndpointIdentifier<Integer> transmissionEndpointIdentifier;

    @Before
    public void setUp() {
        // set up mocks and fields
        userEndpoint = mock(Endpoint.class);
        EndpointScheduler endpointScheduler = new SimpleEndpointScheduler();
        networkEndpointDirectory = mock(EndpointDirectory.class);
        networkEndpointIdentifier = mock(EndpointIdentifier.class);
        networkNode1Endpoint = mock(Endpoint.class);
        networkNode2Endpoint = mock(Endpoint.class);
        doReturn(networkNode1Endpoint).when(networkEndpointDirectory).lookup(1);
        doReturn(networkNode2Endpoint).when(networkEndpointDirectory).lookup(2);
        NonceAccessor<byte[]> nonceAccessor = new ByteArrayNonceAccessor();

        
        // set up builder
        TransmissionActorBuilder<Integer, byte[]> builder = new TransmissionActorBuilder<>(userEndpoint, endpointScheduler,
                networkEndpointDirectory, networkEndpointIdentifier, nonceAccessor);
        builder.addOutgoingRequestType(OutgoingCustomReq.class, Duration.ofMillis(200L), Duration.ofMillis(600L), 3);
        builder.addOutgoingResponseType(OutgoingCustomResp.class, Duration.ofMillis(2000L));
        builder.addIncomingRequestType(IncomingCustomReq.class, Duration.ofMillis(2000L));
        builder.addIncomingResponseType(IncomingCustomResp.class, Duration.ofMillis(2000L));
        
        
        // make actor and initialize
        JavaflowActor transActor = builder.buildActor();
        actorRunnable = ActorRunnable.createAndStart(transActor);
        transEndpoint = actorRunnable.getEndpoint(transActor);
        Object startMsg = builder.buildStartMessage(transEndpoint);
        
        transEndpoint.send(transEndpoint, startMsg);
        
        
        // construction directory/identifier for transmission
        transmissionEndpointDirectory = new TransmissionEndpointDirectory<>(transEndpoint);
        transmissionEndpointIdentifier = new TransmissionEndpointIdentifier<>();
    }

    @After
    public void tearDown() throws InterruptedException {
        actorRunnable.shutdown();
    }

    @Test
    public void testRequestWithoutResponse() throws Exception {        
        // send a message to network node 1... wait for 1 seconds, then make sure the mock was called the 3 times
        Endpoint endpointTo1 = transmissionEndpointDirectory.lookup(1);
        endpointTo1.send(userEndpoint, new OutgoingCustomReq("id-a"));
        
        Thread.sleep(1000L);
        
        verify(networkNode1Endpoint, times(3)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
    }

    @Test
    public void testMultipleRequestsWithoutResponse() throws Exception {        
        // send a message to network node 1+2... wait for 1 seconds, then make sure the mock was called the 3 times for each
        Endpoint endpointTo1 = transmissionEndpointDirectory.lookup(1);
        Endpoint endpointTo2 = transmissionEndpointDirectory.lookup(2);
        endpointTo1.send(userEndpoint, new OutgoingCustomReq("id-a"));
        endpointTo2.send(userEndpoint, new OutgoingCustomReq("id-b"));
        
        Thread.sleep(800L);
        
        verify(networkNode1Endpoint, times(3)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
        verify(networkNode2Endpoint, times(3)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
    }

    @Test
    public void testRequestWithResponse() throws Exception {        
        // send a message to network node 1, have network node 1 respond to it...
        Endpoint userToTransmissionEndpoint = transmissionEndpointDirectory.lookup(1); // used when user sends to transmission
        Endpoint networkToTransmissionEndpoint = new TransmissionInputEndpoint(transEndpoint, 1); // used when network sends to transmission
        
        userToTransmissionEndpoint.send(userEndpoint, new OutgoingCustomReq("id-a"));
        networkToTransmissionEndpoint.send(networkToTransmissionEndpoint, new IncomingCustomResp("id-a"));
        
        Thread.sleep(800L); // sleep to make sure multiple requests aren't sent
        
        verify(networkNode1Endpoint, times(1)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
        verify(userEndpoint, times(1))
                .send(eq(new TransmissionInputEndpoint(transEndpoint, 1)), any(IncomingCustomResp.class));
    }

    @Test
    public void testRequestToSelf() throws Exception {        
        // send a message to network node 1, have network node 1 respond to it...
        Endpoint userToTransmissionEndpoint = transmissionEndpointDirectory.lookup(1); // used when user sends to transmission
        Endpoint networkToTransmissionEndpoint = new TransmissionInputEndpoint(transEndpoint, 1); // used when network sends to transmission
        
        userToTransmissionEndpoint.send(userEndpoint, new OutgoingCustomReq("id-a"));
        networkToTransmissionEndpoint.send(networkToTransmissionEndpoint, new OutgoingCustomReq("id-a"));
        
        Thread.sleep(800L); // sleep to make sure multiple requests aren't sent
        
        verify(networkNode1Endpoint, times(3)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
        verify(userEndpoint, times(0))
                .send(eq(new TransmissionInputEndpoint(transEndpoint, 1)), any(OutgoingCustomReq.class));
    }

    @Test
    public void testDuplicateRequestWithResponse() throws Exception {        
        // send a message to network node 1, have network node 1 respond to it...
        Endpoint userToTransmissionEndpoint = transmissionEndpointDirectory.lookup(1); // used when user sends to transmission
        Endpoint networkToTransmissionEndpoint = new TransmissionInputEndpoint(transEndpoint, 1); // used when network sends to transmission
        
        // send request twice, second request should be ignored, then send response
        userToTransmissionEndpoint.send(userEndpoint, new OutgoingCustomReq("id-a"));
        networkToTransmissionEndpoint.send(networkToTransmissionEndpoint, new IncomingCustomResp("id-a"));
        
        Thread.sleep(100L);
        
        userToTransmissionEndpoint.send(userEndpoint, new OutgoingCustomReq("id-a"));
        
        Thread.sleep(800L); // sleep to make sure multiple requests aren't sent
        
        verify(networkNode1Endpoint, times(1)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
        verify(userEndpoint, times(1))
                .send(eq(new TransmissionInputEndpoint(transEndpoint, 1)), any(IncomingCustomResp.class));
    }

    @Test
    public void testRequestWithDuplicateResponse() throws Exception {        
        // send a message to network node 1, have network node 1 respond to it...
        Endpoint userToTransmissionEndpoint = transmissionEndpointDirectory.lookup(1); // used when user sends to transmission
        Endpoint networkToTransmissionEndpoint = new TransmissionInputEndpoint(transEndpoint, 1); // used when network sends to transmission
        
        // send request, then send response twice ... second response should be ignored
        userToTransmissionEndpoint.send(userEndpoint, new OutgoingCustomReq("id-a"));
        networkToTransmissionEndpoint.send(networkToTransmissionEndpoint, new IncomingCustomResp("id-a"));
        
        Thread.sleep(100L);
        
        networkToTransmissionEndpoint.send(networkToTransmissionEndpoint, new IncomingCustomResp("id-a"));
        
        Thread.sleep(800L); // sleep to make sure multiple requests aren't sent
        
        verify(networkNode1Endpoint, times(1)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
        verify(userEndpoint, times(1))
                .send(eq(new TransmissionInputEndpoint(transEndpoint, 1)), any(IncomingCustomResp.class));
    }

    @Test
    public void testMultipleRequestsWithResponse() throws Exception {        
        // send a message to network node 1+2, have both network nodes respond to it...
        Endpoint userToTransmissionEndpoint1 = transmissionEndpointDirectory.lookup(1);
        Endpoint networkToTransmissionEndpoint1 = new TransmissionInputEndpoint(transEndpoint, 1);
        
        Endpoint userToTransmissionEndpoint2 = transmissionEndpointDirectory.lookup(2);
        Endpoint networkToTransmissionEndpoint2 = new TransmissionInputEndpoint(transEndpoint, 2);
        
        userToTransmissionEndpoint1.send(userEndpoint, new OutgoingCustomReq("id-a"));
        networkToTransmissionEndpoint1.send(networkToTransmissionEndpoint1, new IncomingCustomResp("id-a"));
        
        userToTransmissionEndpoint2.send(userEndpoint, new OutgoingCustomReq("id-b"));
        networkToTransmissionEndpoint2.send(networkToTransmissionEndpoint1, new IncomingCustomResp("id-b"));
        
        Thread.sleep(800L); // sleep to make sure multiple requests aren't sent
        
        verify(networkNode1Endpoint, times(1)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
        verify(networkNode2Endpoint, times(1)).send(eq(transEndpoint), any(OutgoingCustomReq.class));
        verify(userEndpoint, times(1))
                .send(eq(new TransmissionInputEndpoint(transEndpoint, 1)), any(IncomingCustomResp.class));
        verify(userEndpoint, times(1))
                .send(eq(new TransmissionInputEndpoint(transEndpoint, 2)), any(IncomingCustomResp.class));
    }

    protected static final class OutgoingCustomReq extends Request {

        OutgoingCustomReq(String nonce) {
            super(nonce.getBytes(StandardCharsets.UTF_8));
        }
        
        @Override
        protected void innerValidate() {
        }
    }

    protected static final class IncomingCustomReq extends Request {

        IncomingCustomReq(String nonce) {
            super(nonce.getBytes(StandardCharsets.UTF_8));
        }
        
        @Override
        protected void innerValidate() {
        }
    }

    protected static final class OutgoingCustomResp extends Response {

        OutgoingCustomResp(String nonce) {
            super(nonce.getBytes(StandardCharsets.UTF_8));
        }
        
        @Override
        protected void innerValidate() {
        }
    }

    protected static final class IncomingCustomResp extends Response {

        IncomingCustomResp(String nonce) {
            super(nonce.getBytes(StandardCharsets.UTF_8));
        }
        
        @Override
        protected void innerValidate() {
        }
    }
}
