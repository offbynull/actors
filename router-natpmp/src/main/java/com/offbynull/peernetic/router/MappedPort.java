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
package com.offbynull.peernetic.router;

import java.net.InetAddress;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Describes a mapped port.
 *
 * @author Kasra Faghihi
 */
public final class MappedPort {

    private int internalPort;
    private int externalPort;
    private InetAddress externalAddress;
    private PortType portType;

    /**
     * Constructs a {@link MappedPort} object.
     *
     * @param internalPort internal port
     * @param externalPort external port
     * @param externalAddress external address
     * @param portType port type
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is non-positive, or if {@code internalPort > 65535 || externalPort > 65535}
     */
    public MappedPort(int internalPort, int externalPort, InetAddress externalAddress, PortType portType) {
        Validate.inclusiveBetween(1, 65535, internalPort);
        Validate.inclusiveBetween(1, 65535, externalPort);
        Validate.notNull(externalAddress);
        Validate.notNull(portType);

        this.internalPort = internalPort;
        this.externalPort = externalPort;
        this.externalAddress = externalAddress;
        this.portType = portType;
    }

    /**
     * Get internal port.
     *
     * @return internal port
     */
    public int getInternalPort() {
        return internalPort;
    }

    /**
     * Get external port.
     *
     * @return external port
     */
    public int getExternalPort() {
        return externalPort;
    }

    /**
     * Get external address.
     *
     * @return external address
     */
    public InetAddress getExternalAddress() {
        return externalAddress;
    }

    /**
     * Get port type.
     *
     * @return port type
     */
    public PortType getPortType() {
        return portType;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + this.internalPort;
        hash = 31 * hash + this.externalPort;
        hash = 31 * hash + Objects.hashCode(this.externalAddress);
        hash = 31 * hash + Objects.hashCode(this.portType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MappedPort other = (MappedPort) obj;
        if (this.internalPort != other.internalPort) {
            return false;
        }
        if (this.externalPort != other.externalPort) {
            return false;
        }
        if (!Objects.equals(this.externalAddress, other.externalAddress)) {
            return false;
        }
        if (this.portType != other.portType) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MappedPort{" + "internalPort=" + internalPort + ", externalPort=" + externalPort + ", externalAddress=" + externalAddress
                + ", portType=" + portType + '}';
    }

}
