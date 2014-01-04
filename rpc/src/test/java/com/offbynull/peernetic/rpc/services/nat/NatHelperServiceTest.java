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
package com.offbynull.peernetic.rpc.services.nat;

import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.RpcConfig;
import com.offbynull.peernetic.rpc.transport.transports.tcp.TcpTransportFactory;
import java.net.InetSocketAddress;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NatHelperServiceTest {
    
    private Rpc<InetSocketAddress> client;
    private Rpc<InetSocketAddress> server;
    
    public NatHelperServiceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws Throwable {
        TcpTransportFactory factory = new TcpTransportFactory();
        
        factory.setListenAddress(new InetSocketAddress(40990));
        client = new Rpc(factory, new RpcConfig<>());
        
        factory.setListenAddress(new InetSocketAddress(40991));
        server = new Rpc(factory, new RpcConfig<>());
        
        server.addService(NatHelperService.SERVICE_ID, new NatHelperServiceImplementation());
    }
    
    @After
    public void tearDown() throws Throwable {
        client.close();
        server.close();
    }

   @Test
   public void checkForNatTest() throws Throwable {
       NatHelperCallable callable = new NatHelperCallable(client, 40990, new InetSocketAddress("localhost", 40991));
        com.offbynull.peernetic.rpc.services.nat.NatHelperCallable.Result result = callable.call();
       
       Assert.assertTrue(result.getExposedAddress().getAddress().isLoopbackAddress()); // this may not always be true, depends on system?
       Assert.assertTrue(result.isAccessibleTcp());
       Assert.assertTrue(result.isAccessibleUdp());
       Assert.assertTrue(result.isExposedAddressMatchesLocalAddress());
       // Assert.assertTrue(result.isExposedPortMatchesRpcPort()); // this one may be false in some cases
       System.out.println(result);
   }
}
