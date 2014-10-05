package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.SimpleEndpointDirectory;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;

public final class RouterTest {

    @Test
    public void basicTest() throws Exception {
        Endpoint selfEndpoint = Mockito.mock(Endpoint.class);
        Endpoint dstEndpoint = Mockito.mock(Endpoint.class);
        
        Map<Integer, Endpoint> lookupMap = new HashMap<>();
        lookupMap.put(0, selfEndpoint);
        lookupMap.put(1, dstEndpoint);
        EndpointDirectory<Integer> endpointDirectory = new SimpleEndpointDirectory<>(lookupMap);
        
        Router<Integer, byte[]> router = new Router(selfEndpoint, new ByteArrayNonceGenerator(8), new ByteArrayNonceAccessor(),
                endpointDirectory);
        
        Actor subActorOne = Mockito.mock(Actor.class);
        Actor subActorTwo = Mockito.mock(Actor.class);
        
        router.addTypeHandler(subActorOne, FakeRequestOne.class);
        router.addTypeHandler(subActorTwo, FakeRequestTwo.class);
        
        Request req1 = new FakeRequestOne(new byte[] { 0 });
        Request req2 = new FakeRequestTwo(new byte[] { 1 });
        Response resp1 = new FakeResponse();
        Response resp2 = new FakeResponse();
        
        router.routeMessage(Instant.MIN, req1, dstEndpoint);
        router.routeMessage(Instant.MIN, req2, dstEndpoint);
        router.sendResponse(Instant.MIN, req1, resp1, dstEndpoint, Duration.ofSeconds(10L));
        router.sendResponse(Instant.MIN, req2, resp2, dstEndpoint, Duration.ofSeconds(10L));
        
        Mockito.verify(subActorOne, Mockito.times(1)).onStep(Mockito.any(), Mockito.eq(dstEndpoint), Mockito.eq(req1));
        Mockito.verify(subActorTwo, Mockito.times(1)).onStep(Mockito.any(), Mockito.eq(dstEndpoint), Mockito.eq(req2));
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(selfEndpoint, resp1);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(selfEndpoint, resp2);
    }
    
    private static final class FakeRequestOne extends Request {

        public FakeRequestOne(byte[] nonce) {
            super(nonce);
        }

        @Override
        protected void innerValidate() {
            // nothing
        }
    }
    
    private static final class FakeRequestTwo extends Request {

        public FakeRequestTwo(byte[] nonce) {
            super(nonce);
        }

        @Override
        protected void innerValidate() {
            // nothing
        }
    }

    private static final class FakeResponse extends Response {

        public FakeResponse() {
            super();
        }

        @Override
        protected void innerValidate() {
            // nothing
        }
    }
}
