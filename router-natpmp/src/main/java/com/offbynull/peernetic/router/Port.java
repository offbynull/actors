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

import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Encapsulates a port and a protocol (e.g. TCP port 18 / UDP port 10000).
 * @author Kasra Faghihi
 */
public final class Port {
    private PortType portType;
    private int portNumber;

    /**
     * Constructs a {@link Port} object.
     * @param portType port type
     * @param portNumber port number
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code port < 1 || > 65535}
     */
    public Port(PortType portType, int portNumber) {
        Validate.notNull(portType);
        Validate.inclusiveBetween(1, 65535, portNumber);
        this.portType = portType;
        this.portNumber = portNumber;
    }

    /**
     * Get port type.
     * @return port type (e.g. UDP)
     */
    public PortType getPortType() {
        return portType;
    }

    /**
     * Get port number.
     * @return port number
     */
    public int getPortNumber() {
        return portNumber;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.portType);
        hash = 79 * hash + this.portNumber;
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
        final Port other = (Port) obj;
        if (this.portType != other.portType) {
            return false;
        }
        if (this.portNumber != other.portNumber) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Port{" + "portType=" + portType + ", portNumber=" + portNumber + '}';
    }
    
    
}
