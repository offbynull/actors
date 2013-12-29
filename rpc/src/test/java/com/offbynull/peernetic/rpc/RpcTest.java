package com.offbynull.peernetic.rpc;

import com.offbynull.peernetic.rpc.transport.transports.tcp.TcpTransportFactory;
import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RpcTest {

    private static Rpc<InetSocketAddress> rpcSystem;

    public RpcTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        rpcSystem = new Rpc<>(new TcpTransportFactory());
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
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
    
    @Test
    public void listerAsyncServiceTest() throws InterruptedException {
        ListerServiceAsync listerService = rpcSystem.accessService(new InetSocketAddress("localhost", 15000), 0, ListerService.class,
                ListerServiceAsync.class);

        final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        
        listerService.listServices(new AsyncResultListener<ListerService.Services>() {

            @Override
            public void invokationReturned(ListerService.Services object) {
                queue.add(object);
            }

            @Override
            public void invokationThrew(Throwable err) {
                queue.add(err);
            }

            @Override
            public void invokationFailed(Object err) {
                queue.add(err);
            }
        }, 0, Integer.MAX_VALUE);

        ListerService.Services response = (ListerService.Services) queue.poll(1000L, TimeUnit.MILLISECONDS);
        
        Assert.assertEquals(Arrays.asList(0), response.getList());
        Assert.assertEquals(1, response.getTotal());
    }
    
    
    private interface CustomService {
        String echo(String value);
        void throwException(String message);
    }
    
    public final class CustomServiceImplementation implements CustomService {

        @Override
        public String echo(String value) {
            InetSocketAddress address = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
            Assert.assertArrayEquals(new byte[] { 0x7F, 0, 0, 1 }, address.getAddress().getAddress());
            return value;
        }

        @Override
        public void throwException(String message) {
            throw new UnsupportedOperationException(message);
        }
        
    }
    
}
