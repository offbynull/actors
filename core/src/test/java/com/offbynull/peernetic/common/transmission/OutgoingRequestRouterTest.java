package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.NonceGenerator;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.common.transmission.OutgoingRequestRouter.OutgoingRequestRouterController;
import java.time.Instant;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OutgoingRequestRouterTest {
    
    public OutgoingRequestRouterTest() {
    }
    
    @Test
    public void basicTest() throws Exception {
        Endpoint selfEndpoint = mock(Endpoint.class);
        Endpoint dstEndpoint = mock(Endpoint.class);
        EndpointDirectory<Integer> endpointDirectory = mock(EndpointDirectory.class);
        EndpointIdentifier<Integer> endpointIdentifier = mock(EndpointIdentifier.class);
        doReturn(dstEndpoint).when(endpointDirectory).lookup(0);
        doReturn(0).when(endpointIdentifier).identify(dstEndpoint);
        
        NonceAccessor<byte[]> nonceAccessor = new ByteArrayNonceAccessor();
        NonceGenerator<byte[]> nonceGenerator = new ByteArrayNonceGenerator(8);
        OutgoingRequestRouter<Integer, byte[]> outgoingRequestRouter = new OutgoingRequestRouter<>(selfEndpoint, nonceGenerator,
                nonceAccessor, endpointDirectory, endpointIdentifier);
        
        Actor actorOne = mock(Actor.class);
        Actor actorTwo = mock(Actor.class);
        RequestOne requestOne = new RequestOne();
        RequestTwo requestTwo = new RequestTwo();
        Response response = new FakeResponse();
        
        
        Nonce<byte[]> nonceOne = nonceGenerator.generate();
        Nonce<byte[]> nonceTwo = nonceGenerator.generate();
        nonceAccessor.set(requestOne, nonceOne);
        nonceAccessor.set(requestTwo, nonceTwo);
        
        OutgoingRequestRouterController<Integer, byte[]> controllerOne = outgoingRequestRouter.getController(actorOne);
        OutgoingRequestRouterController<Integer, byte[]> controllerTwo = outgoingRequestRouter.getController(actorTwo);
        
        doAnswer((x) -> {
            controllerOne.sendRequest((Instant) x.getArguments()[0], 0, requestOne);
            return null;
        }).when(actorOne).onStep(any(Instant.class), any(Endpoint.class), eq("start"));

        doAnswer((x) -> {
            controllerTwo.sendRequest((Instant) x.getArguments()[0], dstEndpoint, requestTwo);
            return null;
        }).when(actorTwo).onStep(any(Instant.class), any(Endpoint.class), eq("start"));
        
        actorOne.onStep(Instant.MIN, selfEndpoint, "start");
        actorTwo.onStep(Instant.MIN, selfEndpoint, "start");
        
        nonceAccessor.set(response, nonceAccessor.get(requestOne));
        outgoingRequestRouter.route(Instant.MIN, dstEndpoint, response);
        verify(actorOne, times(1)).onStep(any(Instant.class), eq(dstEndpoint), eq(response));
        
        nonceAccessor.set(response, nonceAccessor.get(requestTwo));
        outgoingRequestRouter.route(Instant.MIN, dstEndpoint, response);
        verify(actorTwo, times(1)).onStep(any(Instant.class), eq(dstEndpoint), eq(response));
        
        verify(actorOne, times(1)).onStep(any(Instant.class), eq(dstEndpoint), any(FakeResponse.class));
        verify(actorTwo, times(1)).onStep(any(Instant.class), eq(dstEndpoint), any(FakeResponse.class));
    }
    
    private static final class RequestOne extends Request {

        @Override
        protected void innerValidate() {
        }
    }

    private static final class RequestTwo extends Request {

        @Override
        protected void innerValidate() {
        }
    }

    private static final class FakeResponse extends Response {

        @Override
        protected void innerValidate() {
        }
    }
}
