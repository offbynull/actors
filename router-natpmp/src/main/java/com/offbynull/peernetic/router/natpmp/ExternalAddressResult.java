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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Gateway/router response to a 'get external IP address' request.
 * @author Kasra Faghihi
 */
public final class ExternalAddressResult extends NatPmpResult {
    private long secondsSinceStartOfEpoch;
    private InetAddress address;

    ExternalAddressResult(ByteBuffer buffer) {
        super(buffer);
        
        secondsSinceStartOfEpoch = buffer.getInt() & 0xFFFFFFFFL;
        byte[] addr = new byte[4];
        buffer.get(addr);
        
        try {
            address = InetAddress.getByAddress(addr);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe); // should never happen, will always be 4 bytes
        }
    }

    /**
     * Get the number of seconds since the start of epoch. AKA seconds since this device rebooted.
     * @return number of seconds since start of epoch
     */
    public long getSecondsSinceStartOfEpoch() {
        return secondsSinceStartOfEpoch;
    }

    /**
     * External IP address.
     * @return external IP address
     */
    public InetAddress getAddress() {
        return address;
    }
    
}
