package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.Port;
import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.net.InetAddress;

public class App {
    public static void main(String []args) throws Throwable {
        PortMapper mapper = new PcpPortMapper(new PortMapperEventListener() {

            @Override
            public void resetRequired(String details) {
                System.out.println(details);
            }
        }, InetAddress.getByName("192.168.25.129"), InetAddress.getByName("192.168.25.1"));
        
        MappedPort mappedPort = mapper.mapPort(new Port(PortType.UDP, 10000)).get();
        System.out.println(mappedPort);
        Thread.sleep(200 * 1000L);
        mapper.unmapPort(new Port(PortType.UDP, 10000)).get();
        
        mapper.close();
    }
}
