package com.offbynull.p2prpc;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RpcTest {

    private static Rpc rpcSystem;

    public RpcTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        rpcSystem = new Rpc();
    }

    @AfterClass
    public static void tearDownClass() {
        rpcSystem.close();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void listerServiceTest() {
        ListerService listerService = rpcSystem.accessService(new InetSocketAddress("localhost", 15000), 0, ListerService.class);

        ListerService.Services response = listerService.listServices(0, Integer.MAX_VALUE);

        Assert.assertEquals(Arrays.asList(0), response.getList());
        Assert.assertEquals(1, response.getTotal());
    }

    @Test
    public void customServiceTest() {
        CustomService customServiceImpl = new CustomServiceImplementation();
        rpcSystem.addService(1, customServiceImpl);
        
        CustomService customService = rpcSystem.accessService(new InetSocketAddress("localhost", 15000), 1, CustomService.class);
        String response = customService.echo("this is a test");
        Assert.assertEquals("this is a test", response);
        
        rpcSystem.removeService(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void customServiceThrowTest() {
        CustomService customServiceImpl = new CustomServiceImplementation();
        rpcSystem.addService(1, customServiceImpl);
        
        CustomService customService = rpcSystem.accessService(new InetSocketAddress("localhost", 15000), 1, CustomService.class);
        
        try {
            customService.throwException("this is a test");
        } finally {
            rpcSystem.removeService(1);
        }
    }

    @Test
    public void customServiceDoubleRegisterTest() {
        CustomService customServiceImpl = new CustomServiceImplementation();
        rpcSystem.addService(1, customServiceImpl);
        rpcSystem.addService(2, customServiceImpl);
        
        CustomService customService1 = rpcSystem.accessService(new InetSocketAddress("localhost", 15000), 1, CustomService.class);
        CustomService customService2 = rpcSystem.accessService(new InetSocketAddress("localhost", 15000), 2, CustomService.class);
        
        boolean thrown = false;
        try {
            customService1.throwException("this is a throwable test");
        } catch (UnsupportedOperationException uoe) {
            Assert.assertEquals("this is a throwable test", uoe.getMessage());
            thrown = true;
        }
        Assert.assertTrue(thrown);
        
        String response = customService2.echo("this is an echo test");
        Assert.assertEquals("this is an echo test", response);
        
        rpcSystem.removeService(1);
        rpcSystem.removeService(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void preventRegisteringTheSameIdTwiceTest() {
        CustomService customServiceImpl = new CustomServiceImplementation();
        rpcSystem.addService(1, customServiceImpl);
        
        try {
            rpcSystem.addService(1, customServiceImpl);
        } finally {
            rpcSystem.removeService(1);
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void preventRegistering0Test() {
        CustomService customServiceImpl = new CustomServiceImplementation();
        rpcSystem.addService(0, customServiceImpl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void preventDeregistering0Test() {
        rpcSystem.removeService(0);
    }
    
    private interface CustomService {
        String echo(String value);
        void throwException(String message);
    }
    
    public final class CustomServiceImplementation implements CustomService {

        @Override
        public String echo(String value) {
            return value;
        }

        @Override
        public void throwException(String message) {
            throw new UnsupportedOperationException(message);
        }
        
    }
    
}
