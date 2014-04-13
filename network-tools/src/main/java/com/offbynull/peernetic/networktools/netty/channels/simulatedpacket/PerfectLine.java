/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.networktools.netty.channels.simulatedpacket;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A line implementation without any delays, constraints, or potential failures. As soon as a packet comes in, it arrives as its
 * destination.
 * @author Kasra Faghihi
 */
public final class PerfectLine implements Line {

    @Override
    public Collection<TransitPacket> depart(long timestamp, SocketAddress from, SocketAddress to, ByteBuffer data) {
        return new ArrayList<>(Arrays.asList(new TransitPacket(from, to, data, Long.MIN_VALUE)));
    }

    @Override
    public Collection arrive(long timestamp, Collection<TransitPacket> packets) {
        return packets;
    }
    
}
