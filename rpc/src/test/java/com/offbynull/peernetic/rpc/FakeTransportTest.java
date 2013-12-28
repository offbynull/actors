package com.offbynull.peernetic.rpc;

import com.offbynull.peernetic.rpc.transport.CompositeIncomingFilter;
import com.offbynull.peernetic.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessage;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.peernetic.rpc.transport.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessage;
import com.offbynull.peernetic.rpc.transport.OutgoingResponse;
import com.offbynull.peernetic.rpc.transport.TerminateIncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.TransportHelper;
import com.offbynull.peernetic.rpc.transports.test.TestHub;
import com.offbynull.peernetic.rpc.transports.test.TestTransport;
import com.offbynull.peernetic.rpc.transports.test.Line;
import com.offbynull.peernetic.rpc.transports.test.PerfectLine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class FakeTransportTest {
    
    private static final IncomingFilter<Integer> EMPTY_INCOMING_FILTER =
            new CompositeIncomingFilter<>(Collections.<IncomingFilter<Integer>>emptyList());
    private static final OutgoingFilter<Integer> EMPTY_OUTGOING_FILTER =
            new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<Integer>>emptyList());

    private TestHub<Integer> hub;
    private TestTransport<Integer> transport1;
    private TestTransport<Integer> transport2;

    public FakeTransportTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        Line<Integer> line = new PerfectLine<>();
        hub = new TestHub(line);
        hub.start();
        
        transport1 = new TestTransport<>(1, hub, 500L);
        transport2 = new TestTransport<>(2, hub, 500L);
    }

    @After
    public void tearDown() throws IOException {
        hub.stop();
    }

    @Test
    public void selfFakeTest() throws Throwable {
        IncomingMessageListener<Integer> listener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(IncomingMessage<Integer> message, IncomingMessageResponseHandler responseCallback) {
                ByteBuffer buffer = message.getData();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                if (new String(data).equals("HIEVERYBODY! :)")) {
                    responseCallback.responseReady(new OutgoingResponse("THIS IS THE RESPONSE".getBytes()));
                } else {
                    responseCallback.terminate();
                }
            }
        };

        try {
            transport1.start(EMPTY_INCOMING_FILTER, listener, EMPTY_OUTGOING_FILTER);

            Integer to = 1;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<Integer> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<Integer> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse.getData());
        } finally {
            transport1.stop();
        }
    }

    @Test
    public void normalFakeTest() throws Throwable {
        IncomingMessageListener<Integer> listener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(IncomingMessage<Integer> message, IncomingMessageResponseHandler responseCallback) {
                ByteBuffer buffer = message.getData();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                if (new String(data).equals("HIEVERYBODY! :)")) {
                    responseCallback.responseReady(new OutgoingResponse("THIS IS THE RESPONSE".getBytes()));
                } else {
                    responseCallback.terminate();
                }
            }
        };

        try {
            transport1.start(EMPTY_INCOMING_FILTER, listener, EMPTY_OUTGOING_FILTER);
            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<Integer>(), EMPTY_OUTGOING_FILTER);

            Integer to = 1;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<Integer> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<Integer> incomingResponse = TransportHelper.sendAndWait(transport2, outgoingMessage);

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse.getData());
        } finally {
            transport1.stop();
            transport2.stop();
        }
    }

    @Test
    public void terminatedFakeTest() throws Throwable {
        IncomingMessageListener<Integer> listener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(IncomingMessage<Integer> message, IncomingMessageResponseHandler responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport1.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<Integer>(), EMPTY_OUTGOING_FILTER);
            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<Integer>(), EMPTY_OUTGOING_FILTER);

            Integer to = 2;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<Integer> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<Integer> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);

            Assert.assertNull(incomingResponse);
        } finally {
            transport1.stop();
            transport2.stop();
        }
    }

    @Test
    public void noResponseFakeTest() throws Throwable {
        try {
            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<Integer>(), EMPTY_OUTGOING_FILTER);
            
            Integer to = 6;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<Integer> outgoingMessage = new OutgoingMessage<>(to, data);

            IncomingResponse<Integer> incomingResponse = TransportHelper.sendAndWait(transport2, outgoingMessage);

            Assert.assertNull(incomingResponse);
        } finally {
            transport2.stop();
        }
    }
}
