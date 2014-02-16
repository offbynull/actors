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
package com.offbynull.peernetic.router.natpmp;

import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.net.InetAddress;

/**
 * NAT-PMP test.
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
                //}, InetAddress.getByName("192.168.25.129"), InetAddress.getByName("192.168.25.1")); //miniupnpd details
        PortMapper mapper = new NatPmpPortMapper(InetAddress.getByName("10.0.1.1"), new PortMapperEventListener() { // NOPMD

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
