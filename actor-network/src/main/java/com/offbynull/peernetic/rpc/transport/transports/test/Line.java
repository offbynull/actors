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
package com.offbynull.peernetic.rpc.transport.transports.test;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Controls how messages are sent in a hub. For example, depending on the line, a message may be dropped/duplicated/corrupted/slow/fast...
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface Line<A> {
    /**
     * Generate {@link TransitMessage} objects for an outgoing message.
     * @param from source
     * @param to destination
     * @param data contents
     * @return list of {@link TransitMessage} objects
     */
    Collection<TransitMessage<A>> depart(A from, A to, ByteBuffer data);
    
    /**
     * Signals that {@link TransitMessage} objects that were created by this line have arrived. Implementations can modify the arrivals.
     * @param messages {@link TransitMessage} objects that have arrived
     * @return modified messages
     */
    Collection<TransitMessage<A>> arrive(Collection<TransitMessage<A>> messages);
}
