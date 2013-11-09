package com.offbynull.p2prpc;

import com.offbynull.p2prpc.session.ServerMessageCallback;
import com.offbynull.p2prpc.session.ServerResponseCallback;
import com.offbynull.p2prpc.session.TcpClient;
import com.offbynull.p2prpc.session.TcpServer;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

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
        TcpTransport base = new TcpTransport(12345);
        base.start();
        
        TcpServer server = new TcpServer(base);
        TcpClient client = new TcpClient(base);
        
        ServerMessageCallback<InetSocketAddress> mockedCallback = Mockito.mock(ServerMessageCallback.class);
        server.start(mockedCallback);
        
        client.send(new InetSocketAddress("localhost", 12345), "HIEVERYBODY! :)".getBytes(), 500L);
        
        Mockito.verify(mockedCallback).messageArrived(
                Matchers.any(InetSocketAddress.class),
                Matchers.eq("HIEVERYBODY! :)".getBytes()),
                Matchers.any(ServerResponseCallback.class));
        
        server.stop();
        base.stop();
    }

    @Test
    public void unconnectableTcpTest() throws Throwable {
        TcpTransport base = new TcpTransport(12345);
        base.start();
        
        TcpServer server = new TcpServer(base);
        TcpClient client = new TcpClient(base);
        
        ServerMessageCallback<InetSocketAddress> mockedCallback = Mockito.mock(ServerMessageCallback.class);
        server.start(mockedCallback);
        
        client.send(new InetSocketAddress("34h93hfouehf", 12345), "HIEVERYBODY! :)".getBytes(), 500L);
        
        Mockito.verify(mockedCallback, Mockito.never()).messageArrived(
                Matchers.any(InetSocketAddress.class),
                Matchers.any(byte[].class),
                Matchers.any(ServerResponseCallback.class));
        
        server.stop();
        base.stop();
    }
}
