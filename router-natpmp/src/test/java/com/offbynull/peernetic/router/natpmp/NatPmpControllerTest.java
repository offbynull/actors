package com.offbynull.peernetic.router.natpmp;

import com.offbynull.peernetic.router.testtools.UdpServerEmulator;
import com.offbynull.peernetic.router.PortType;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NatPmpControllerTest {

    private UdpServerEmulator helper;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Throwable {
        helper = UdpServerEmulator.create(5351);
    }

    @After
    public void tearDown() throws Throwable {
        helper.close();
    }

    @Test
    public void exposedAddressTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 0}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128, 0, 0, 0, 0, 0, 0, 127, 0, 0, 1}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        ExternalAddressResponse res = controller.getExternalAddress();
        InetAddress address = res.getAddress();
        
        Assert.assertEquals(InetAddress.getLoopbackAddress(), address);
    }

    @Test(expected = IOException.class)
    public void failedExposedAddressTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 0}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128, 0, 1, 0, 0, 0, 0, 127, 0, 0, 1}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        controller.getExternalAddress();
    }

    
    @Test(expected = IOException.class)
    public void truncatedExposedAddressTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 0}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128, 0, 0}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        controller.getExternalAddress();
    }
    
    @Test
    public void openUdpPortTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 1, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 10}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        CreateMappingResponse res = controller.createMapping(PortType.UDP, 1, 2, 9);

        Assert.assertEquals(3, res.getExternalPort());
        Assert.assertEquals(1, res.getInternalPort());
        Assert.assertEquals(10, res.getLifetime());
    }
    
    @Test
    public void openTcpPortTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 2, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 10}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        CreateMappingResponse res = controller.createMapping(PortType.TCP, 1, 2, 9);

        Assert.assertEquals(3, res.getExternalPort());
        Assert.assertEquals(1, res.getInternalPort());
        Assert.assertEquals(10, res.getLifetime());
    }
    
    @Test(expected = IOException.class)
    public void openUdpPortWrongPortTypeResponseTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 1, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 10}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        CreateMappingResponse res = controller.createMapping(PortType.UDP, 1, 2, 9);

        Assert.assertEquals(3, res.getExternalPort());
        Assert.assertEquals(1, res.getInternalPort());
        Assert.assertEquals(10, res.getLifetime());
    }
    
    @Test(expected = IOException.class)
    public void openTcpPortWrongPortTypeResponseTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 2, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 10}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        CreateMappingResponse res = controller.createMapping(PortType.TCP, 1, 2, 9);

        Assert.assertEquals(3, res.getExternalPort());
        Assert.assertEquals(1, res.getInternalPort());
        Assert.assertEquals(10, res.getLifetime());
    }

    @Test(expected = IOException.class)
    public void failedOpenUdpPortTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 1, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 10}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        controller.createMapping(PortType.UDP, 1, 2, 9);
    }

    @Test(expected = IOException.class)
    public void failedOpenTcpPortTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 2, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 2, 0, 1, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 10}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        controller.createMapping(PortType.TCP, 1, 2, 9);
    }

    
    @Test(expected = IOException.class)
    public void truncatedOpenUdpPortTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 1, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 1, 0, 0}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        controller.createMapping(PortType.UDP, 1, 2, 9);
    }

    @Test(expected = IOException.class)
    public void truncatedOpenTcpPortTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {0, 2, 0, 0, 0, 1, 0, 2, 0, 0, 0, 9}),
                ByteBuffer.wrap(new byte[] {0, (byte) 128 + 2, 0, 0}));
        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.1"), 4);
        
        controller.createMapping(PortType.TCP, 1, 2, 9);
    }

    @Test(expected = IOException.class)
    public void timedOutTest() throws Throwable {        
        NatPmpController controller = new NatPmpController(InetAddress.getByName("127.0.0.2"), 4);
        
        controller.createMapping(PortType.TCP, 1, 2, 9);
    }
}
