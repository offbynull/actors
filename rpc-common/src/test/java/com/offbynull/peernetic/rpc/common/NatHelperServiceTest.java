/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.offbynull.peernetic.rpc.common;

import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.RpcConfig;
import com.offbynull.peernetic.rpc.TcpTransportFactory;
import com.offbynull.peernetic.rpc.common.services.nat.NatHelperService;
import com.offbynull.peernetic.rpc.common.services.nat.NatHelperServiceImplementation;
import com.offbynull.peernetic.rpc.common.services.nat.NatHelperCallable;
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
        com.offbynull.peernetic.rpc.common.services.nat.NatHelperCallable.Result result = callable.call();
       
       Assert.assertTrue(result.getExposedAddress().getAddress().isLoopbackAddress()); // this may not always be true, depends on system?
       Assert.assertTrue(result.isAccessibleTcp());
       Assert.assertTrue(result.isAccessibleUdp());
       Assert.assertTrue(result.isExposedAddressMatchesLocalAddress());
       // Assert.assertTrue(result.isExposedPortMatchesRpcPort()); // this one may be false in some cases
       System.out.println(result);
   }
}
