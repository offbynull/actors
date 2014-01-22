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

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Gateway/router response to a 'new port mapping' request.
 * @author Kasra Faghihi
 */
public final class CreateMappingResult extends NatPmpResult {
    private long secondsSinceStartOfEpoch;
    private int internalPort;
    private int externalPort;
    private long lifetime;

    CreateMappingResult(ByteBuffer buffer, int expectedInternalPort) {
        super(buffer);
        
        secondsSinceStartOfEpoch = buffer.getInt() & 0xFFFFFFFFL;
        internalPort = buffer.getShort() & 0xFFFF;
        externalPort = buffer.getShort() & 0xFFFF;
        lifetime = buffer.getInt() & 0xFFFFFFFFL;
        
        Validate.isTrue(expectedInternalPort == internalPort);
        Validate.inclusiveBetween(1, 65535, externalPort);
        Validate.inclusiveBetween(1L, 0xFFFFFFFFL, lifetime);
    }

    /**
     * Get the number of seconds since the start of epoch. AKA seconds since this device rebooted.
     * @return number of seconds since start of epoch
     */
    public long getSecondsSinceStartOfEpoch() {
        return secondsSinceStartOfEpoch;
    }

    /**
     * Get the internal/local port number.
     * @return internal/local port number
     */
    public int getInternalPort() {
        return internalPort;
    }

    /**
     * Get the external/remote port number.
     * @return external/remote port number
     */
    public int getExternalPort() {
        return externalPort;
    }

    /**
     * Get the lifetime for this mapping.
     * @return lifetime for this mapping
     */
    public long getLifetime() {
        return lifetime;
    }
    
}
