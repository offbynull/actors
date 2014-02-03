package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.common.PortType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

public class App {

    public static void main(String[] args) throws Throwable {
//        PcpCommunicator comm = new PcpCommunicator(InetAddress.getByName("192.168.25.129"));
////        PcpCommunicator comm = new PcpCommunicator(InetAddress.getByName("fe80::20c:29ff:fe90:9de9"));
//        comm.startAsync().awaitRunning();
//        comm.addListener(new PcpResponseListener() {
//
//            @Override
//            public void incomingPacket(InetSocketAddress from, PcpResponseListener.CommunicationType type, ByteBuffer packet) {
//                System.out.println(from);
//            }
//        });
//        
//        ByteBuffer sendBuffer = ByteBuffer.allocate(1100);
//        MapPcpRequest req = new MapPcpRequest(ByteBuffer.allocate(12), 6, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
//        req.dump(sendBuffer, InetAddress.getByName("192.168.10.1"));
//        
//        sendBuffer.flip();
//        comm.send(sendBuffer);
//        
//        Thread.sleep(1000L);
//        
//        comm.stopAsync().awaitTerminated();
        
        
        
        
//        NatPmpController controller = new NatPmpController(InetAddress.getByName("192.168.25.129"), 4);
//        InetAddress address = controller.getExternalAddress().getAddress();
//        
//        System.out.println(address);
//
        PcpController controller = new PcpController(new Random(), InetAddress.getByName("192.168.25.129"), InetAddress.getByName("192.168.25.1"), 4, null);
//        PeerPcpResponse response;
//        
//        response = controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 12345, InetAddress.getByName("1.1.1.1"), 100);
        MapPcpResponse response;
//
        response = controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
        response = controller.createInboundMapping(PortType.TCP, 12345, 12346, InetAddress.getByName("192.168.10.129"), 100,
                new PreferFailurePcpOption());
//        
//        
//        AnnouncePcpResponse response = controller.announce();
//        MapPcpResponse response = controller.createInboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.10.129"), 100);
//        PeerPcpResponse response = controller.createOutboundMapping(PortType.TCP, 12345, 12345, InetAddress.getByName("192.168.25.1"),
//                0, InetAddress.getByName("128.1.128.1"), 100);
//
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
