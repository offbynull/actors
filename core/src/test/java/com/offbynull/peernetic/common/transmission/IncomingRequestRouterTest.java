package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.NonceGenerator;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.common.transmission.IncomingRequestRouter.IncomingRequestRouterController;
import java.time.Instant;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IncomingRequestRouterTest {

    public IncomingRequestRouterTest() {
    }

    @Test
    public void basicTest() throws Exception {
        Endpoint selfEndpoint = mock(Endpoint.class);
        Endpoint dstEndpoint = mock(Endpoint.class);
        EndpointDirectory<Integer> endpointDirectory = mock(EndpointDirectory.class);
        doReturn(dstEndpoint).when(endpointDirectory).lookup(0);
        
        NonceAccessor<byte[]> nonceAccessor = new ByteArrayNonceAccessor();
        NonceGenerator<byte[]> nonceGenerator = new ByteArrayNonceGenerator(8);
        IncomingRequestRouter<Integer, byte[]> incomingRequestRouter = new IncomingRequestRouter<>(selfEndpoint, nonceAccessor,
                endpointDirectory);

        Actor actorOne = mock(Actor.class);
        Actor actorTwo = mock(Actor.class);
        RequestOne requestOne = new RequestOne();
        RequestTwo requestTwo = new RequestTwo();
        Response response = new FakeResponse();
        
        
        Nonce<byte[]> nonceOne = nonceGenerator.generate();
        Nonce<byte[]> nonceTwo = nonceGenerator.generate();
        nonceAccessor.set(requestOne, nonceOne);
        nonceAccessor.set(requestTwo, nonceTwo);

        IncomingRequestRouterController<Integer, byte[]> controllerOne = incomingRequestRouter.getController(actorOne);
        IncomingRequestRouterController<Integer, byte[]> controllerTwo = incomingRequestRouter.getController(actorTwo);

        controllerOne.registerType(requestOne.getClass());
        controllerTwo.registerType(requestTwo.getClass());

        doAnswer((x) -> {
            controllerOne.sendResponse((Instant) x.getArguments()[0], 0, x.getArguments()[2], response);
            return null;
        }).when(actorOne).onStep(any(Instant.class), any(Endpoint.class), eq(requestOne));

        doAnswer((x) -> {
            controllerTwo.sendResponse((Instant) x.getArguments()[0], dstEndpoint, x.getArguments()[2], response);
            return null;
        }).when(actorTwo).onStep(any(Instant.class), any(Endpoint.class), eq(requestTwo));
        
        
        incomingRequestRouter.route(Instant.MIN, dstEndpoint, requestOne);
        incomingRequestRouter.route(Instant.MIN, dstEndpoint, requestTwo);
        
        verify(actorOne, times(1)).onStep(any(Instant.class), any(Endpoint.class), eq(requestOne));
        verify(actorTwo, times(1)).onStep(any(Instant.class), any(Endpoint.class), eq(requestTwo));
        verify(dstEndpoint, times(2)).send(selfEndpoint, response);
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
