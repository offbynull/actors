package com.offbynull.rpc;

import com.offbynull.rpc.transport.CompositeIncomingFilter;
import com.offbynull.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.rpc.transport.IncomingFilter;
import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageListener;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.rpc.transport.IncomingResponse;
import com.offbynull.rpc.transport.OutgoingFilter;
import com.offbynull.rpc.transport.OutgoingMessage;
import com.offbynull.rpc.transport.OutgoingResponse;
import com.offbynull.rpc.transport.TerminateIncomingMessageListener;
import com.offbynull.rpc.transport.TransportHelper;
import com.offbynull.rpc.transport.fake.FakeHub;
import com.offbynull.rpc.transport.fake.FakeTransport;
import com.offbynull.rpc.transport.fake.Line;
import com.offbynull.rpc.transport.fake.PerfectLine;
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

    private FakeHub<Integer> hub;
    private FakeTransport<Integer> transport1;
    private FakeTransport<Integer> transport2;

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
        hub = new FakeHub(line);
        hub.start();
        
        transport1 = new FakeTransport<>(1, hub, 500L);
        transport2 = new FakeTransport<>(2, hub, 500L);
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
