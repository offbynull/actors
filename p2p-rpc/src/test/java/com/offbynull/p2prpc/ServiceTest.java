package com.offbynull.p2prpc;

import com.offbynull.p2prpc.service.ListerService;
import com.offbynull.p2prpc.service.ListerService.Response;
import com.offbynull.p2prpc.service.ServiceAccessor;
import com.offbynull.p2prpc.service.ServiceServer;
import com.offbynull.p2prpc.session.Client;
import com.offbynull.p2prpc.session.Server;
import com.offbynull.p2prpc.session.SessionedClient;
import com.offbynull.p2prpc.session.SessionedServer;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class ServiceTest {
    
    private static final long TIMEOUT = 5000;

    private static AtomicInteger nextPort;
    
    private Server<InetSocketAddress> server;
    private ServiceServer<InetSocketAddress> serviceServer;
    private ServiceAccessor<InetSocketAddress> serviceAccessor;
    private TcpTransport serverTransport;
    private int serverPort;
    
    private Client<InetSocketAddress> client;
    private TcpTransport clientTransport;
    private int clientPort;

    public ServiceTest() {
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
        serverPort = nextPort.getAndIncrement();
        serverTransport = new TcpTransport(serverPort);
        serverTransport.start();
        
        server = new SessionedServer<>(serverTransport, TIMEOUT);
        serviceServer = new ServiceServer<>(server, Executors.newSingleThreadExecutor());
        serviceServer.start();
        
        
        clientPort = nextPort.getAndIncrement();
        clientTransport = new TcpTransport(clientPort);
        clientTransport.start();

        client = new SessionedClient<>(clientTransport);
        serviceAccessor = new ServiceAccessor<>(client);
    }

    @After
    public void tearDown() throws IOException {
        serviceServer.stop();
        serverTransport.stop();
        clientTransport.stop();
    }


    @Test
    public void simpleServiceTest() throws Throwable {
        ListerService listerService = serviceAccessor.accessService(new InetSocketAddress(serverPort), 0, ListerService.class);
        
        Response response = listerService.query(0, Integer.MAX_VALUE);
        
        Assert.assertEquals(Arrays.asList(0), response.getList());
        Assert.assertEquals(1, response.getTotal());
    }
}
