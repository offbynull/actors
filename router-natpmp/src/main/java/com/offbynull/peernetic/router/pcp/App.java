package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.common.PortType;
import com.offbynull.peernetic.router.natpmp.NatPmpController;
import com.offbynull.peernetic.router.natpmp.NatPmpUtils;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class App {

    public static void main(String[] args) throws Throwable {
//        NatPmpController controller = new NatPmpController(InetAddress.getByName("192.168.25.129"), 4);
//        InetAddress address = controller.getExternalAddress().getAddress();
//        
//        System.out.println(address);
        
        PcpController controller = new PcpController(InetAddress.getByName("192.168.25.129"), InetAddress.getByName("192.168.25.1"), 4);
//        AnnouncePcpResponse response = controller.announce();
        MapPcpResponse response = controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
//        PeerPcpResponse response = controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.25.1"),
//                0, InetAddress.getByName("128.1.128.1"), 100);
        
        System.out.println(response);
    }
}
