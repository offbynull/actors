package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.natpmp.NatPmpUtils;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class App {

    public static void main(String[] args) throws Throwable {
//        System.out.println(NatPmpUtils.findGateway());
        
        DatagramSocket datagramSocket = new DatagramSocket(10001);//10000 + new Random().nextInt(55535));

//        ByteBuffer nonce = ByteBuffer.allocate(12);
//        MapPcpRequest request = new MapPcpRequest(nonce, 17, 10001, 10001, InetAddress.getByName("::ffff:0:0"), 30000L);
//        ByteBuffer requestBuffer = ByteBuffer.allocate(1100);
//        request.dump(requestBuffer, InetAddress.getByName("192.168.1.246"));
//        requestBuffer.flip();
//        DatagramPacket requestDp = new DatagramPacket(new byte[] { 0, 0 }, 2, InetAddress.getByName("192.168.1.1"), 5351);
//        datagramSocket.send(requestDp);


        ByteBuffer responseBuffer = ByteBuffer.allocate(1100);
        DatagramPacket responseDp = new DatagramPacket(responseBuffer.array(), responseBuffer.limit());
        datagramSocket.receive(responseDp);
        
        responseBuffer.limit(responseDp.getLength());
        MapPcpResponse response = new MapPcpResponse(responseBuffer);
        
        System.out.println(response);
    }
}
