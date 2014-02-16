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
package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.common.CommunicationType;
import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * A PCP {@link PortMapper} implementation.
 *
 * @author Kasra Faghihi
 */
public final class PcpPortMapper implements PortMapper {

    private PcpController controller;
    private boolean preferIpv6External;
    private volatile boolean closed;

    public PcpPortMapper(InetAddress gatewayAddress, InetAddress selfAddress, boolean preferIpv6External,
            final PortMapperEventListener listener) {
        Validate.notNull(gatewayAddress);
        Validate.notNull(selfAddress);
        Validate.notNull(listener);

        this.preferIpv6External = preferIpv6External;

        controller = new PcpController(new Random(), gatewayAddress, selfAddress, new PcpControllerListener() {

            @Override
            public void incomingResponse(CommunicationType type, PcpResponse response) {
                if (closed) {
                    return;
                }

                if (response instanceof AnnouncePcpResponse) {
                    listener.resetRequired("Mappings may have been lost.");
                }
            }
        });
    }

    @Override
    public MappedPort mapPort(PortType portType, int internalPort, long lifetime) throws InterruptedException {
        Validate.validState(!closed);
        Validate.notNull(portType);
        Validate.inclusiveBetween(1, 65535, internalPort);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, lifetime);

        lifetime = Math.min(lifetime, (long) Integer.MAX_VALUE); // cap lifetime

        InetAddress externalAddress;
        try {
            if (preferIpv6External) {
                externalAddress = InetAddress.getByName("::");
            } else {
                externalAddress = InetAddress.getByName("::ffff:0:0");
            }
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }

        try {
            MapPcpResponse resp = controller.requestMapOperation(4, portType, internalPort, 0, externalAddress, lifetime);

            return new MappedPort(resp.getInternalPort(), resp.getAssignedExternalPort(), resp.getAssignedExternalIpAddress(),
                    PortType.fromIanaNumber(resp.getProtocol()), resp.getLifetime());
        } catch (RuntimeException re) {
            throw new IllegalStateException(re);
        }
    }

    @Override
    public void unmapPort(PortType portType, int internalPort) throws InterruptedException {
        Validate.validState(!closed);
        Validate.inclusiveBetween(1, 65535, internalPort);

        InetAddress externalAddress;
        try {
            externalAddress = InetAddress.getByName("::");
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }

        try {
            controller.requestMapOperation(4, portType, internalPort, 0, externalAddress, 0L);
        } catch (RuntimeException re) {
            throw new IllegalStateException(re);
        }
    }

    @Override
    public MappedPort refreshPort(MappedPort mappedPort, long lifetime) throws InterruptedException {
        Validate.validState(!closed);
        Validate.notNull(mappedPort);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, lifetime);
        
        lifetime = Math.min(lifetime, (long) Integer.MAX_VALUE); // cap lifetime
        
        MappedPort newMappedPort;
        try {
            MapPcpResponse resp = controller.requestMapOperation(4, mappedPort.getPortType(), mappedPort.getInternalPort(),
                    mappedPort.getExternalPort(), mappedPort.getExternalAddress(), lifetime);//, new PreferFailurePcpOption());
            // Preferfailurd does not work on Apple Airport Extreme :( unsupp_option.

            newMappedPort = new MappedPort(resp.getInternalPort(), resp.getAssignedExternalPort(),
                    resp.getAssignedExternalIpAddress(), PortType.fromIanaNumber(resp.getProtocol()), resp.getLifetime());
        } catch (RuntimeException re) {
            throw new IllegalStateException(re);
        }
        
        try {
            Validate.isTrue(mappedPort.getExternalAddress().equals(mappedPort.getExternalAddress()), "External address changed");
            Validate.isTrue(mappedPort.getInternalPort() == mappedPort.getInternalPort(), "External port changed");
        } catch (IllegalStateException ise) {
            // port has been mapped to different external ip and/or port, unmap and return error
            try {
                unmapPort(newMappedPort.getPortType(), newMappedPort.getInternalPort());
            } catch (RuntimeException re) { // NOPMD
                // do nothing
            }
            
            throw ise;
        }


        return newMappedPort;
    }
    
    @Override
    public void close() throws IOException {
        closed = true;
        controller.close();
    }
}
