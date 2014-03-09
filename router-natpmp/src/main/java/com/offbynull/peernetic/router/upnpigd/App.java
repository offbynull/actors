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
        UpnpIgdController controller = new UpnpIgdController(InetAddress.getByName("192.168.25.1"), service);
        
        //System.out.println(controller.getExternalIp());
        
        controller.addPortMapping(23422, 12221, PortType.TCP, 3600);
        System.out.println(controller.getMappingDetails(23422, PortType.TCP));
        controller.deletePortMapping(23422, PortType.TCP);
        System.out.println(controller.getMappingDetails(23422, PortType.TCP));
    }
}
