package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
import java.time.Duration;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public final class IncomingRequestManagerTest {

    @Test
    public void basicTest() throws Exception {
        Endpoint srcEndpoint = Mockito.mock(Endpoint.class);
        Endpoint dstEndpoint = Mockito.mock(Endpoint.class);
        NonceAccessor<byte[]> nonceAccessor = new ByteArrayNonceAccessor();
        IncomingRequestManager<String, byte[]> incomingRequestManager = new IncomingRequestManager<>(srcEndpoint, nonceAccessor);
        
        Request request = new FakeRequest(new byte[] { 1, 2, 3 });
        Response response = new FakeResponse();
        
        
        Instant nextInstant = Instant.ofEpochSecond(0L);
        Duration duration;
        
        Assert.assertTrue(incomingRequestManager.testRequestMessage(nextInstant, request));
        incomingRequestManager.track(nextInstant, request, dstEndpoint, Duration.ofSeconds(60L));
        incomingRequestManager.respond(nextInstant, request, response, dstEndpoint);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(srcEndpoint, response);
        Assert.assertFalse(incomingRequestManager.testRequestMessage(nextInstant, request));
        Mockito.verify(dstEndpoint, Mockito.times(2)).send(srcEndpoint, response);
        Assert.assertFalse(incomingRequestManager.testRequestMessage(nextInstant, request));
        Mockito.verify(dstEndpoint, Mockito.times(3)).send(srcEndpoint, response);
        
        duration = incomingRequestManager.process(nextInstant);
        Assert.assertEquals(Duration.ofSeconds(60L), duration);
        
        nextInstant = Instant.ofEpochSecond(60L);
        duration = incomingRequestManager.process(nextInstant);
        Assert.assertNull(duration);
    }
    
    private static final class FakeRequest extends Request {

        public FakeRequest(byte[] nonce) {
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
