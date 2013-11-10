package com.offbynull.p2prpc;

import com.offbynull.p2prpc.session.PacketIdGenerator;
import com.offbynull.p2prpc.session.ServerMessageCallback;
import com.offbynull.p2prpc.session.ServerResponseCallback;
import com.offbynull.p2prpc.transport.udp.UdpTransport;
import com.offbynull.p2prpc.session.NonSessionedClient;
import com.offbynull.p2prpc.session.NonSessionedServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class UdpTest {

    private static final long TIMEOUT = 500;

    private UdpTransport transport1;
    private UdpTransport transport2;
    private int port1;
    private int port2;
    private static AtomicInteger nextPort;

    public UdpTest() {
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
        port2 = nextPort.getAndIncrement();
        transport1 = new UdpTransport(port1, 65535);
        transport2 = new UdpTransport(port2, 65535);
        transport1.start();
        transport2.start();
    }

    @After
    public void tearDown() throws IOException {
        transport1.stop();
        transport2.stop();
    }

    @Test
    public void selfUdpTest() throws Throwable {
        NonSessionedServer server = null;

        try {
            server = new NonSessionedServer(transport1, TIMEOUT);
            NonSessionedClient client = new NonSessionedClient(transport1, new PacketIdGenerator());

            ServerMessageCallback callback = new ServerMessageCallback() {

                @Override
                public void messageArrived(Object from, byte[] data, ServerResponseCallback responseCallback) {
                    if (new String(data).equals("HIEVERYBODY! :)")) {
                        responseCallback.responseReady("THIS IS THE RESPONSE".getBytes());
                    } else {
                        responseCallback.terminate();
                    }
                }
            };

            server.start(callback);

            byte[] response = client.send(new InetSocketAddress("localhost", port1), "HIEVERYBODY! :)".getBytes(), TIMEOUT);
            
            Assert.assertArrayEquals("THIS IS THE RESPONSE".getBytes(), response);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    @Test
    public void normalUdpTest() throws Throwable {
        NonSessionedServer server = null;

        try {
            server = new NonSessionedServer(transport1, TIMEOUT);
            ServerMessageCallback callback = new ServerMessageCallback() {

                @Override
                public void messageArrived(Object from, byte[] data, ServerResponseCallback responseCallback) {
                    if (new String(data).equals("HIEVERYBODY! :)")) {
                        responseCallback.responseReady("THIS IS THE RESPONSE".getBytes());
                    } else {
                        responseCallback.terminate();
                    }
                }
            };
            server.start(callback);

            NonSessionedClient client = new NonSessionedClient(transport2, new PacketIdGenerator());
            byte[] response = client.send(new InetSocketAddress("localhost", port1), "HIEVERYBODY! :)".getBytes(), TIMEOUT);

            Assert.assertArrayEquals("THIS IS THE RESPONSE".getBytes(), response);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    @Test(expected = IOException.class)
    public void noResponseUdpTest() throws Throwable {
        NonSessionedClient client = new NonSessionedClient(transport2, new PacketIdGenerator());

        client.send(new InetSocketAddress("www.microsoft.com", 12345), "HIEVERYBODY! :)".getBytes(), 500L);
    }
}
