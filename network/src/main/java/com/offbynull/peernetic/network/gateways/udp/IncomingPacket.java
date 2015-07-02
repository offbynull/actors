/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.network.gateways.udp;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

final class IncomingPacket {
    private final byte[] packet;
    private final InetSocketAddress sourceSocketAddress;

    public IncomingPacket(byte[] packet, InetSocketAddress sourceSocketAddress) {
        Validate.notNull(packet);
        Validate.notNull(sourceSocketAddress);
        
        this.packet = Arrays.copyOf(packet, packet.length);
        this.sourceSocketAddress = sourceSocketAddress;
    }

    public byte[] getPacket() {
        return Arrays.copyOf(packet, packet.length);
    }

    public InetSocketAddress getSourceSocketAddress() {
        return sourceSocketAddress;
    }

    @Override
    public String toString() {
        return "IncomingPacket{" + "packet=" + Arrays.toString(packet) + ", sourceSocketAddress=" + sourceSocketAddress + '}';
    }
    
}
