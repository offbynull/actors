package com.offbynull.peernetic.router.natpmp;

import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.net.InetAddress;

public class App {
    public static void main(String []args) throws Throwable {
                //}, InetAddress.getByName("192.168.25.129"), InetAddress.getByName("192.168.25.1")); //miniupnpd details
        PortMapper mapper = new NatPmpPortMapper(InetAddress.getByName("10.0.1.1"), new PortMapperEventListener() {

            @Override
            public void resetRequired(String details) {
                System.out.println(details);
            }
        });
        
        MappedPort mappedPort;
        
        mappedPort = mapper.mapPort(PortType.UDP, 10000, 30);
        System.out.println(mappedPort);
        Thread.sleep(mappedPort.getLifetime() * 1000L / 2L);
        
        mappedPort = mapper.refreshPort(mappedPort, 40);
        System.out.println(mappedPort);
        Thread.sleep(mappedPort.getLifetime() * 1000L);
        
        mapper.unmapPort(PortType.UDP, 10000);
        
        
        
        mapper.close();
    }
}
