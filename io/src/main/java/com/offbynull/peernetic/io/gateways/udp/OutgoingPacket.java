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
package com.offbynull.peernetic.io.gateways.udp;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

final class OutgoingPacket {
    private final byte[] packet;
    private final InetSocketAddress destinationSocketAddress;

    public OutgoingPacket(byte[] packet, InetSocketAddress destinationSocketAddress) {
        Validate.notNull(packet);
        Validate.notNull(destinationSocketAddress);
        
        this.packet = Arrays.copyOf(packet, packet.length);
        this.destinationSocketAddress = destinationSocketAddress;
    }

    public byte[] getPacket() {
        return Arrays.copyOf(packet, packet.length);
    }

    public InetSocketAddress getDestinationSocketAddress() {
        return destinationSocketAddress;
    }

    @Override
    public String toString() {
        return "OutgoingPacket{" + "packet=" + Arrays.toString(packet) + ", destinationSocketAddress=" + destinationSocketAddress + '}';
    }
    
}
