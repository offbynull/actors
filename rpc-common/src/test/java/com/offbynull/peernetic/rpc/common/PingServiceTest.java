package com.offbynull.peernetic.rpc.common;

import com.offbynull.peernetic.rpc.FakeTransportFactory;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.transports.fake.FakeHub;
import com.offbynull.peernetic.rpc.transports.fake.PerfectLine;
import com.offbynull.peernetic.rpc.common.services.ping.PingCallable;
import com.offbynull.peernetic.rpc.common.services.ping.PingService;
import com.offbynull.peernetic.rpc.common.services.ping.PingServiceImplementation;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class PingServiceTest {
    
    public PingServiceTest() {
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
    public void testPingService() throws Throwable {
        FakeHub<Integer> hub = new FakeHub<>(new PerfectLine<Integer>());
        hub.start();
        
        Rpc<Integer> rpc1 = new Rpc<>(new FakeTransportFactory<>(hub, 1));
        Rpc<Integer> rpc2 = new Rpc<>(new FakeTransportFactory<>(hub, 2));
        
        rpc2.addService(PingService.SERVICE_ID, new PingServiceImplementation());
        
        PingCallable<Integer> callable = new PingCallable<>(2, rpc1);
        long rtt = callable.call();
        
        Assert.assertTrue(rtt >= 0L);
        
        hub.stop();
    }
}
