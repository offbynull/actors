package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.testtools.UdpTestUtils;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class PcpReceiverTest {
    
    public PcpReceiverTest() {
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
    @Ignore // ignored because "gateway address" is non-deterministic -- address of this machine on a local interface
    public void receiverIpv4Test() throws Throwable {
        final PcpReceiver receiver = new PcpReceiver(InetAddress.getByName("192.168.25.1"));
        final PcpEventListener listener = Mockito.mock(PcpEventListener.class);
        
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    receiver.start(listener);
                } catch (Exception e) {
                    Assert.fail();
                } finally {
                    receiver.stop();
                } 
            }
        };
        
        thread.start();
        Thread.sleep(500L); // make sure thread is listening
        
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {
            2, // version
            (byte) 128, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 0, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
        });
        UdpTestUtils.sendMulticast(InetAddress.getByName("224.0.0.1"), 5350, buffer);
        
        try {
            Mockito.verify(listener, Mockito.timeout(1000)).incomingEvent(Mockito.any(AnnouncePcpResponse.class));
        } finally {
            receiver.stop();
            thread.join();
        }
    }
    
    @Test
    @Ignore // ignored because "gateway address" is non-deterministic -- address of this machine on a local interface
    public void receiverIpv6Test() throws Throwable {
        final PcpReceiver receiver = new PcpReceiver(InetAddress.getByName("fe80:0:0:0:fd40:c842:7926:64cb%35"));
        final PcpEventListener listener = Mockito.mock(PcpEventListener.class);
        
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    receiver.start(listener);
                } catch (Exception e) {
                    Assert.fail();
                } finally {
                    receiver.stop();
                } 
            }
        };
        
        thread.start();
        Thread.sleep(500L); // make sure thread is listening
        
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {
            2, // version
            (byte) 128, // opcode + rflag
            0, // reserved
            0, // result code (success)
            0, 0, 0, 0, // lifetime
            0, 0, 0, 1, // epoch time
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
        });
        UdpTestUtils.sendMulticast(InetAddress.getByName("ff02::1"), 5350, buffer);
        
        try {
            Mockito.verify(listener, Mockito.timeout(1000)).incomingEvent(Mockito.any(AnnouncePcpResponse.class));
        } finally {
            receiver.stop();
            thread.join();
        }
    }
}
