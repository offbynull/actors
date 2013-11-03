package com.offbynull.p2prpc;

import com.offbynull.p2prpc.session.PacketIdGenerator;
import com.offbynull.p2prpc.session.ServerMessageCallback;
import com.offbynull.p2prpc.session.ServerResponseCallback;
import com.offbynull.p2prpc.transport.UdpTransport;
import com.offbynull.p2prpc.session.UdpClient;
import com.offbynull.p2prpc.session.UdpServer;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

public class UdpTest {
    
    public UdpTest() {
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
    public void simpleUdpTest() throws Throwable {
        UdpTransport base = new UdpTransport(12345);
        base.start();
        
        UdpServer server = new UdpServer(base);
        UdpClient client = new UdpClient(base, new PacketIdGenerator());
        
        ServerMessageCallback<SocketAddress> mockedCallback = Mockito.mock(ServerMessageCallback.class);
        server.start(mockedCallback);
        
        client.send(new InetSocketAddress("localhost", 12345), "HIEVERYBODY! :)".getBytes(), 10000L);
        
        Mockito.verify(mockedCallback).messageArrived(
                Matchers.any(SocketAddress.class),
                Matchers.eq("HIEVERYBODY! :)".getBytes()),
                Matchers.any(ServerResponseCallback.class));
        
        server.stop();
        base.stop();
    }
}
