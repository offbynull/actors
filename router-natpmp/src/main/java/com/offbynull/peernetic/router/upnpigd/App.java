/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.router.upnpigd;

import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.net.InetAddress;
import java.util.Set;

/**
 * UPnP-IGD test.
 * @author Kasra Faghihi
 */
public final class App {
    private App() {
        // do nothing
    }
    
    /**
     * Main method.
     * @param args unused
     * @throws Throwable on error
     */
    public static void main(String []args) throws Throwable {
        Set<UpnpIgdService> services = UpnpIgdDiscovery.discover();
        UpnpIgdService service = services.iterator().next();
        
        PortMapper mapper = new UpnpIgdPortMapper(service, InetAddress.getByName("192.168.25.1"), new PortMapperEventListener() { // NOPMD

            @Override
            public void resetRequired(String details) {
                System.out.println(details);
                System.exit(0);
            }
        });
        
        
        //System.out.println(controller.getExternalIp());
        
        MappedPort mappedPort = mapper.mapPort(PortType.TCP, 12345, 10L);
        for (int i = 0; i < 3; i++) {
            Thread.sleep(5000L);
            System.out.println("Refreshing...");
            mapper.refreshPort(mappedPort, 10L);
        }
        
        Thread.sleep(20000L);
        
        mapper.close();
    }
}
