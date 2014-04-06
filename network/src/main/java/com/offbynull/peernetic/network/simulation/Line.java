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
package com.offbynull.peernetic.network.simulation;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Defines the properties of a packet as it enters a {@link TransitPacketRepository}. For example, depending on the implementation of this
 * interface, a packet may be dropped/duplicated/corrupted/slow/fast/etc..
 * @author Kasra Faghihi
 */
public interface Line {
    /**
     * Generate {@link TransitPacket} objects for an outgoing packet.
     * @param timestamp current timestamp
     * @param from source
     * @param to destination
     * @param data packet contents
     * @return a collection of one or more {@link TransitPacket} objects
     */
    Collection<TransitPacket> depart(long timestamp, SocketAddress from, SocketAddress to, ByteBuffer data);
    
    /**
     * Signals that {@link TransitPacket} objects that were created by this line have arrived.
     * @param timestamp current timestamp
     * @param messages {@link TransitPacket} objects that have arrived
     * @return a collection of one or more {@link TransitPacket} objects
     */
    Collection<TransitPacket> arrive(long timestamp, Collection<TransitPacket> messages);
}
