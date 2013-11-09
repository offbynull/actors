package com.offbynull.p2prpc;

import com.offbynull.p2prpc.session.ServerMessageCallback;
import com.offbynull.p2prpc.session.ServerResponseCallback;
import com.offbynull.p2prpc.session.SessionedClient;
import com.offbynull.p2prpc.session.SessionedServer;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class TcpTest {

    public TcpTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void selfTcpTest() throws Throwable {
        int port = 12346;
        TcpTransport base = new TcpTransport(port);
        base.start();
        
        SessionedServer server = new SessionedServer(base);
        SessionedClient client = new SessionedClient(base);
        
        ServerMessageCallback<InetSocketAddress> mockedCallback = Mockito.mock(ServerMessageCallback.class);
        server.start(mockedCallback);
        
        client.send(new InetSocketAddress("localhost", port), "HIEVERYBODY! :)".getBytes(), 500L);
        
        Mockito.verify(mockedCallback).messageArrived(
                Matchers.any(InetSocketAddress.class),
                Matchers.eq("HIEVERYBODY! :)".getBytes()),
                Matchers.any(ServerResponseCallback.class));
        
        server.stop();
        base.stop();
    }

    @Test(expected = IOException.class)
    public void unconnectableTcpTest() throws Throwable {
        int port = 12345;
        TcpTransport base = new TcpTransport(port);
        base.start();
        
        SessionedServer server = new SessionedServer(base);
        SessionedClient client = new SessionedClient(base);
        
        ServerMessageCallback<InetSocketAddress> mockedCallback = Mockito.mock(ServerMessageCallback.class);
        server.start(mockedCallback);
        
        client.send(new InetSocketAddress("34h93hfouehf", port), "HIEVERYBODY! :)".getBytes(), 500L);
        
        Mockito.verify(mockedCallback, Mockito.never()).messageArrived(
                Matchers.any(InetSocketAddress.class),
                Matchers.any(byte[].class),
                Matchers.any(ServerResponseCallback.class));
        
        server.stop();
        base.stop();
    }
}
