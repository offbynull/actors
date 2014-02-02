package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.common.BadResponseException;
import com.offbynull.peernetic.router.common.PortType;
import com.offbynull.peernetic.router.testtools.UdpServerEmulator;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public final class PcpControllerTest {
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
    public void announceTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            0, // opcode
            0, 0, // reserved
            0, 0, 0, 0, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 127, 0, 0, 1, // from ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            (byte) 128, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 0, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.0.0.1"), 4);
        
        AnnouncePcpResponse response = controller.announce();
        
        Assert.assertEquals(0L, response.getLifetime());
        Assert.assertEquals(1L, response.getEpochTime());
        Assert.assertEquals(0, response.getOp());
        Assert.assertTrue(response.getOptions().isEmpty());
    }

    @Test(expected = BadResponseException.class)
    public void failedAnnounceTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            0, // opcode
            0, 0, // reserved
            0, 0, 0, 0, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 127, 0, 0, 1, // from ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            (byte) 128, // opcode + rflag
            0, // reserved
            1, // result code (fail)
            0, 0, 0, 0, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.0.0.1"), 4);
        
        controller.announce();
    }
    
    @Test(expected = BadResponseException.class)
    public void truncatedAnnounceTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            0, // opcode
            0, 0, // reserved
            0, 0, 0, 0, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 127, 0, 0, 1, // from ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            (byte) 128, // opcode + rflag
            0, // reserved
            1, // result code (fail)
            0, 0, 0, 0, // lifetime
            0, 0, 0, 1, // epoch time
            0, // 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.0.0.1"), 4);
        
        controller.announce();
    }
    
    @Test
    public void mapTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            1, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            -127, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 120, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // protocol
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // external port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -128 // external ip
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        MapPcpResponse response = controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);

        Assert.assertEquals(120L, response.getLifetime());
        Assert.assertEquals(1L, response.getEpochTime());
        Assert.assertEquals(1, response.getOp());
        Assert.assertEquals(12345, response.getAssignedExternalPort());
        Assert.assertEquals(InetAddress.getByName("192.168.10.128"), response.getAssignedExternalIpAddress());
        Assert.assertEquals(12345, response.getInternalPort());
        Assert.assertTrue(response.getOptions().isEmpty());
    }
    
    @Test
    public void mapWithPreferFailureTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            1, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ip
            2, 0, 0, 0, // prefer failure option
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            -127, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 120, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // protocol
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // external port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -128, // external ip
            2, 0, 0, 0 // prefer failure
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        MapPcpResponse response = controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100, new PreferFailurePcpOption());

        Assert.assertEquals(120L, response.getLifetime());
        Assert.assertEquals(1L, response.getEpochTime());
        Assert.assertEquals(1, response.getOp());
        Assert.assertEquals(12345, response.getAssignedExternalPort());
        Assert.assertEquals(InetAddress.getByName("192.168.10.128"), response.getAssignedExternalIpAddress());
        Assert.assertEquals(12345, response.getInternalPort());
        Assert.assertEquals(1, response.getOptions().size());
        Assert.assertEquals(PreferFailurePcpOption.class, response.getOptions().get(0).getClass());
    }

    @Test(expected = BadResponseException.class)
    public void failedMapTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            1, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            -127, // opcode + rflag
            0, // reserved
            1, // result code (failed)
            0, 0, 0, 120, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
    }

    @Test(expected = BadResponseException.class)
    public void truncatedMapTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            1, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            -127, // opcode + rflag
            0, // reserved
            0, // result code
            0, 0, 0, 120, // lifetime
            0, 0,
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
    }
    
    @Test
    public void peerTest() throws Throwable {
        
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            2, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested ext port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ext ip
            48, 57, // remote peer port
            0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 1, 1, 1 // remote peer ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            -126, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 120, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // protocol
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // assign ext port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -128, // assigned ext ip
            48, 57, // assigned remote peer port
            0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 1, 1, 1 // remote peer ip
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        PeerPcpResponse response = controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 12345, InetAddress.getByName("1.1.1.1"), 100);

        Assert.assertEquals(120L, response.getLifetime());
        Assert.assertEquals(1L, response.getEpochTime());
        Assert.assertEquals(2, response.getOp());
        Assert.assertEquals(12345, response.getAssignedExternalPort());
        Assert.assertEquals(InetAddress.getByName("192.168.10.128"), response.getAssignedExternalIpAddress());
        Assert.assertEquals(12345, response.getInternalPort());
        Assert.assertEquals(12345, response.getRemotePeerPort());
        Assert.assertEquals(InetAddress.getByName("1.1.1.1"), response.getRemotePeerIpAddress());
        Assert.assertTrue(response.getOptions().isEmpty());
    }

    @Test(expected = BadResponseException.class)
    public void peerBadVersionTest() throws Throwable {
        
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            2, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested ext port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ext ip
            48, 57, // remote peer port
            0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 1, 1, 1 // remote peer ip
        }), ByteBuffer.wrap(new byte[] {
            3, // version
            -126, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 120, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // protocol
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // assign ext port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -128, // assigned ext ip
            48, 57, // assigned remote peer port
            0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 1, 1, 1 // remote peer ip
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        PeerPcpResponse response = controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 12345, InetAddress.getByName("1.1.1.1"), 100);

        Assert.assertEquals(120L, response.getLifetime());
        Assert.assertEquals(1L, response.getEpochTime());
        Assert.assertEquals(2, response.getOp());
        Assert.assertEquals(12345, response.getAssignedExternalPort());
        Assert.assertEquals(InetAddress.getByName("192.168.10.128"), response.getAssignedExternalIpAddress());
        Assert.assertEquals(12345, response.getInternalPort());
        Assert.assertEquals(12345, response.getRemotePeerPort());
        Assert.assertEquals(InetAddress.getByName("1.1.1.1"), response.getRemotePeerIpAddress());
        Assert.assertTrue(response.getOptions().isEmpty());
    }


    @Test(expected = BadResponseException.class)
    public void failedPeerTest() throws Throwable {
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            2, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested ext port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ext ip
            48, 57, // remote peer port
            0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 1, 1, 1 // remote peer ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            -126, // opcode + rflag
            0, // reserved
            1, // result code (failed)
            0, 0, 0, 120, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 12345, InetAddress.getByName("1.1.1.1"), 100);
    }

    @Test(expected = BadResponseException.class)
    public void truncatedPeerTest() throws Throwable {
        
        helper.addMapping(ByteBuffer.wrap(new byte[] {
            2, // version
            2, // opcode
            0, 0, // reserved
            0, 0, 0, 100, // lifetime
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 25, 1, // from ip
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // tcp
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // suggested ext port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -127, // suggested ext ip
            48, 57, // remote peer port
            0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 1, 1, 1 // remote peer ip
        }), ByteBuffer.wrap(new byte[] {
            2, // version
            -126, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 120, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // nonce
            6, // protocol
            0, 0, 0, // reserved
            48, 57, // internal port
            48, 57, // assign ext port
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, -88, 10, -128, // assigned ext ip
            48, //57, // assigned remote peer port
            }));
        
        PcpController controller = new PcpController(Mockito.mock(Random.class), InetAddress.getByName("127.0.0.1"), InetAddress.getByName("192.168.25.1"), 4);

        controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 12345, InetAddress.getByName("1.1.1.1"), 100);
    }
}
