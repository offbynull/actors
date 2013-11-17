package com.offbynull.p2prpc;

import com.offbynull.p2prpc.transport.udp.UdpTransport;
import com.offbynull.p2prpc.transport.IncomingMessage;
import com.offbynull.p2prpc.transport.IncomingMessageListener;
import com.offbynull.p2prpc.transport.IncomingMessageResponseHandler;
import com.offbynull.p2prpc.transport.IncomingResponse;
import com.offbynull.p2prpc.transport.OutgoingMessage;
import com.offbynull.p2prpc.transport.OutgoingResponse;
import com.offbynull.p2prpc.transport.TransportHelper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class UdpTransportTest {

    private static final long TIMEOUT_DURATION = 500;
    private static final int BUFFER_SIZE = 500;
    private static final int ID_CACHE_SIZE = 1024;

    private UdpTransport transport1;
    private UdpTransport transport2;
    private int port1;
    private int port2;
    private static AtomicInteger nextPort;

    public UdpTransportTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        nextPort = new AtomicInteger(12000);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        port1 = nextPort.getAndIncrement();
        transport1 = new UdpTransport(port1, BUFFER_SIZE, ID_CACHE_SIZE, TIMEOUT_DURATION);
        transport1.start();
        
        port2 = nextPort.getAndIncrement();
        transport2 = new UdpTransport(port2, BUFFER_SIZE, ID_CACHE_SIZE, TIMEOUT_DURATION);
        transport2.start();
    }

    @After
    public void tearDown() throws IOException {
        transport1.stop();
        transport2.stop();
    }

    @Test
    public void selfUdpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
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
            transport1.addMessageListener(listener);

            InetSocketAddress to = new InetSocketAddress("localhost", port1);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse.getData());
        } finally {
            transport1.removeMessageListener(listener);
        }
    }

    @Test
    public void normalUdpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
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
            transport2.addMessageListener(listener);

            InetSocketAddress to = new InetSocketAddress("localhost", port2);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse.getData());
        } finally {
            transport2.removeMessageListener(listener);
        }
    }

    @Test
    public void terminatedUdpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport2.addMessageListener(listener);

            InetSocketAddress to = new InetSocketAddress("localhost", port2);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);

            Assert.assertNull(incomingResponse);
        } finally {
            transport2.removeMessageListener(listener);
        }
    }

    @Test
    public void noResponseUdpTest() throws Throwable {
        InetSocketAddress to = new InetSocketAddress("www.microsoft.com", 12345);
        byte[] data = "HIEVERYBODY! :)".getBytes();
        OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);

        IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport2, outgoingMessage);
        
        Assert.assertNull(incomingResponse);
    }
}
