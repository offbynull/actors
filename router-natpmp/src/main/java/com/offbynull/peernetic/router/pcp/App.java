package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.common.PortType;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

public class App {

    public static void main(String[] args) throws Throwable {
//        NatPmpController controller = new NatPmpController(InetAddress.getByName("192.168.25.129"), 4);
//        InetAddress address = controller.getExternalAddress().getAddress();
//        
//        System.out.println(address);

        PcpController controller = new PcpController(new Random(), InetAddress.getByName("192.168.25.129"), InetAddress.getByName("192.168.25.1"), 4);
        PeerPcpResponse response;
        
        response = controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 12345, InetAddress.getByName("1.1.1.1"), 100);
//        MapPcpResponse response;
//
//        response = controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
//        response = controller.createInboundMapping(PortType.TCP, 12345, 12346, InetAddress.getByName("192.168.10.129"), 100,
//                new PreferFailurePcpOption());
        
        
//        AnnouncePcpResponse response = controller.announce();
//        MapPcpResponse response = controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
//        PeerPcpResponse response = controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.25.1"),
//                0, InetAddress.getByName("128.1.128.1"), 100);

        System.out.println(response);
    }

    public static String bytesToHex(ByteBuffer bytes) {
        StringBuilder sb = new StringBuilder();
        for (int j = bytes.position(); j < bytes.limit(); j++) {
            sb.append(", ").append(bytes.get(j));
        }
        return sb.toString();
    }
}
