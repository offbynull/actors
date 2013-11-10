package com.offbynull.p2prpc;

import com.offbynull.p2prpc.session.ServerMessageCallback;
import com.offbynull.p2prpc.session.ServerResponseCallback;
import com.offbynull.p2prpc.session.SessionedClient;
import com.offbynull.p2prpc.session.SessionedServer;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TcpTest {

    private static final long TIMEOUT = 500;

    private TcpTransport transport1;
    private TcpTransport transport2;
    private int port1;
    private int port2;
    private static AtomicInteger nextPort;

    public TcpTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        nextPort = new AtomicInteger(13000);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        port1 = nextPort.getAndIncrement();
        port2 = nextPort.getAndIncrement();
        transport1 = new TcpTransport(port1);
        transport2 = new TcpTransport(port2);
        transport1.start();
        transport2.start();
    }

    @After
    public void tearDown() throws IOException {
        transport1.stop();
        transport2.stop();
    }

    @Test
    public void selfTcpTest() throws Throwable {
        SessionedServer server = null;

        try {
            server = new SessionedServer(transport1, TIMEOUT);
            SessionedClient client = new SessionedClient(transport1);

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
    public void normalTcpTest() throws Throwable {
        SessionedServer server = null;

        try {
            server = new SessionedServer(transport1, TIMEOUT);
            SessionedClient client = new SessionedClient(transport2);

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

            server.stop();
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    @Test(expected = IOException.class)
    public void noResponseTcpTest() throws Throwable {
        SessionedClient client = new SessionedClient(transport2);

        client.send(new InetSocketAddress("www.microsoft.com", 12345), "HIEVERYBODY! :)".getBytes(), TIMEOUT);
    }
}
