/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc.transport.services.ping;

import com.offbynull.peernetic.rpc.transport.transports.test.TestTransportFactory;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.common.services.ping.PingCallable;
import com.offbynull.peernetic.rpc.common.services.ping.PingService;
import com.offbynull.peernetic.rpc.common.services.ping.PingServiceImplementation;
import com.offbynull.peernetic.rpc.transport.transports.test.PerfectLine;
import com.offbynull.peernetic.rpc.transport.transports.test.TestHub;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        TestHub<Integer> hub = new TestHub<>(new PerfectLine<Integer>());
        hub.start();
        
        Rpc<Integer> rpc1 = new Rpc<>(new TestTransportFactory<>(hub, 1));
        Rpc<Integer> rpc2 = new Rpc<>(new TestTransportFactory<>(hub, 2));
        
        rpc2.addService(PingService.SERVICE_ID, new PingServiceImplementation());
        
        PingCallable<Integer> callable = new PingCallable<>(2, rpc1);
        long rtt = callable.call();
        
        Assert.assertTrue(rtt >= 0L);
        
        hub.stop();
    }
}
