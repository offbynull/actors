package com.offbynull.peernetic.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public final class BasicUdpGatewayTest {
    
    @Test
    public void basicUdpGatewayTest() throws Throwable {
        InetSocketAddress address1 = new InetSocketAddress(InetAddress.getLocalHost(), 9000);
        List<Object> incoming1 = new ArrayList<>();
        UdpGateway udpGateway1 = new UdpGateway(
                address1,
                (m) -> {
                    incoming1.add(m.getMessage());
                },
                new XStreamSerializer());
        InetSocketAddress address2 = new InetSocketAddress(InetAddress.getLocalHost(), 9001);
        List<Object> incoming2 = new ArrayList<>();
        UdpGateway udpGateway2 = new UdpGateway(
                address2,
                (m) -> {
                    incoming2.add(m.getMessage());
                },
                new XStreamSerializer());
        
        Thread.sleep(1000L);
        
        udpGateway1.send(address2, "m1->2");
        udpGateway2.send(address1, "m2->1");

        Thread.sleep(1000L);
        
        udpGateway1.close();
        udpGateway2.close();
        
        
        Assert.assertEquals(Arrays.asList((Object) "m1->2"), incoming2);
        Assert.assertEquals(Arrays.asList((Object) "m2->1"), incoming1);
    }
}
