package com.offbynull.peernetic.common;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import java.time.Duration;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public final class OutgoingRequestManagerTest {
    
    private static final String DST_ADDRESS = "dstAddress";
    
    @Test
    public void noResponseTest() throws Exception {
        Endpoint srcEndpoint = Mockito.mock(Endpoint.class);
        Endpoint dstEndpoint = Mockito.mock(Endpoint.class);
        NonceGenerator<byte[]> nonceGenerator = new ByteArrayNonceGenerator(8);
        NonceWrapper<byte[]> nonceWrapper = new ByteArrayNonceWrapper();
        EndpointDirectory<String> endpointDirectory = Mockito.mock(EndpointDirectory.class);
        
        Mockito.when(endpointDirectory.lookup(DST_ADDRESS)).thenReturn(dstEndpoint);
        
        Request request = new FakeRequest();
        
        // initial send at 0L
        OutgoingRequestManager<String, byte[]> outgoingRequestManager = new OutgoingRequestManager(srcEndpoint, nonceGenerator,
                nonceWrapper, endpointDirectory);
        outgoingRequestManager.sendRequestAndTrack(Instant.ofEpochSecond(0L), request, DST_ADDRESS);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(srcEndpoint, request); // verify initial send
        
        
        Instant nextInstant = Instant.ofEpochSecond(0L);
        Duration duration;
        
        // no send at 0L -- haven't hit resend time at 5L yet
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(srcEndpoint, request);
        Assert.assertEquals(Instant.ofEpochSecond(5L), nextInstant);
        
        // yes send at 5L
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(10L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(2)).send(srcEndpoint, request);
        
        // yes send at 10L
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(15L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(3)).send(srcEndpoint, request);

        // no send at 15L
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(30L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(4)).send(srcEndpoint, request);
        
        // no send at 30L -- should be discarded
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request)); // should still be true - process() not called yet
                                                                                          // with 30L
        duration = outgoingRequestManager.process(nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(4)).send(srcEndpoint, request);
        Assert.assertNull(duration);
        
        // no send at 12345L -- should be discarded
        nextInstant = Instant.ofEpochSecond(12345L);
        Assert.assertFalse(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(4)).send(srcEndpoint, request);
        Assert.assertNull(duration);
    }

    @Test
    public void withMatchingResponseTest() throws Exception {
        Endpoint srcEndpoint = Mockito.mock(Endpoint.class);
        Endpoint dstEndpoint = Mockito.mock(Endpoint.class);
        NonceGenerator<byte[]> nonceGenerator = new ByteArrayNonceGenerator(8);
        NonceWrapper<byte[]> nonceWrapper = new ByteArrayNonceWrapper();
        EndpointDirectory<String> endpointDirectory = Mockito.mock(EndpointDirectory.class);
        
        Mockito.when(endpointDirectory.lookup(DST_ADDRESS)).thenReturn(dstEndpoint);
        
        Request request = new FakeRequest();
        
        // initial send at 0L
        OutgoingRequestManager<String, byte[]> outgoingRequestManager = new OutgoingRequestManager(srcEndpoint, nonceGenerator,
                nonceWrapper, endpointDirectory);
        outgoingRequestManager.sendRequestAndTrack(Instant.ofEpochSecond(0L), request, DST_ADDRESS);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(srcEndpoint, request); // verify initial send
        
        Response response = new FakeResponse(request.getNonce()); // after adding to outgoingRequestManager, nonce should be set, so
                                                                  // copy that in to fake response
        
        Instant nextInstant = Instant.ofEpochSecond(0L);
        Duration duration;
        
        // no send at 0L -- haven't hit resend time at 5L yet
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(srcEndpoint, request);
        Assert.assertEquals(Instant.ofEpochSecond(5L), nextInstant);
        
        // yes send at 5L
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(10L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(2)).send(srcEndpoint, request);
        
        // push in response at 5L
        nextInstant = Instant.ofEpochSecond(5L);
        Assert.assertTrue(outgoingRequestManager.testResponseMessage(nextInstant, response));
        Assert.assertFalse(outgoingRequestManager.testResponseMessage(nextInstant, response)); // should be false if done again
        
        
        // try again at 5L, should be no send and no next process time because of response
        Assert.assertFalse(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        Assert.assertNull(duration);
        Mockito.verify(dstEndpoint, Mockito.times(2)).send(srcEndpoint, request);

        // no send at 12345L -- should be discarded
        nextInstant = Instant.ofEpochSecond(12345L);
        Assert.assertFalse(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(2)).send(srcEndpoint, request);
        Assert.assertNull(duration);
    }

    @Test
    public void withNonMatchingResponseTest() throws Exception {
        Endpoint srcEndpoint = Mockito.mock(Endpoint.class);
        Endpoint dstEndpoint = Mockito.mock(Endpoint.class);
        NonceGenerator<byte[]> nonceGenerator = new ByteArrayNonceGenerator(8);
        NonceWrapper<byte[]> nonceWrapper = new ByteArrayNonceWrapper();
        EndpointDirectory<String> endpointDirectory = Mockito.mock(EndpointDirectory.class);
        
        Mockito.when(endpointDirectory.lookup(DST_ADDRESS)).thenReturn(dstEndpoint);
        
        Request request = new FakeRequest();
        
        // initial send at 0L
        OutgoingRequestManager<String, byte[]> outgoingRequestManager = new OutgoingRequestManager(srcEndpoint, nonceGenerator,
                nonceWrapper, endpointDirectory);
        outgoingRequestManager.sendRequestAndTrack(Instant.ofEpochSecond(0L), request, DST_ADDRESS);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(srcEndpoint, request); // verify initial send
        
        Response response = new FakeResponse(new byte[] { -1 }); // will never match nonce of request, because nonce of request is 8 bytes
        
        Instant nextInstant = Instant.ofEpochSecond(0L);
        Duration duration;
        
        // no send at 0L -- haven't hit resend time at 5L yet
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Mockito.verify(dstEndpoint, Mockito.times(1)).send(srcEndpoint, request);
        Assert.assertEquals(Instant.ofEpochSecond(5L), nextInstant);
        
        // yes send at 5L
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(10L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(2)).send(srcEndpoint, request);
        
        // push in bad response at 5L
        nextInstant = Instant.ofEpochSecond(5L);
        Assert.assertFalse(outgoingRequestManager.testResponseMessage(nextInstant, response));
        
        // try again at 5L and make sure that its still good
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(10L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(2)).send(srcEndpoint, request);
        
        // yes send at 10L
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(15L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(3)).send(srcEndpoint, request);

        // no send at 15L
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        nextInstant = nextInstant.plus(duration);
        Assert.assertEquals(Instant.ofEpochSecond(30L), nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(4)).send(srcEndpoint, request);
        
        // no send at 30L -- should be discarded
        Assert.assertTrue(outgoingRequestManager.isMessageTracked(nextInstant, request)); // should still be true - process() not called yet
                                                                                          // with 30L
        duration = outgoingRequestManager.process(nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(4)).send(srcEndpoint, request);
        Assert.assertNull(duration);
        
        // no send at 12345L -- should be discarded
        nextInstant = Instant.ofEpochSecond(12345L);
        Assert.assertFalse(outgoingRequestManager.isMessageTracked(nextInstant, request));
        duration = outgoingRequestManager.process(nextInstant);
        Mockito.verify(dstEndpoint, Mockito.times(4)).send(srcEndpoint, request);
        Assert.assertNull(duration);
    }

    private static final class FakeRequest extends Request {

        public FakeRequest() {
            super();
        }

        @Override
        protected void innerValidate() {
            // nothing
        }
    }

    private static final class FakeResponse extends Response {

        public FakeResponse(byte[] nonce) {
            super(nonce);
        }

        @Override
        protected void innerValidate() {
            // nothing
        }
    }
    
}
