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
package com.offbynull.actors.core.gateway;

import com.offbynull.actors.core.shuttle.Shuttle;

/**
 * A gateway, like an actor, communicates with other components through message-passing, but isn't bound by any of the same rules as
 * actors. Gateways are mainly used to interface with third-party components. As such, it's perfectly acceptable for a gateway to expose
 * internal state, share state, perform I/O, perform thread synchronization, or otherwise block.
 * 
 * For example, a gateway could ...
 * 
 * <ul>
 * <li>read messages from a TCP connection and forward them.</li>
 * <li>read messages from a file and forward them.</li>
 * <li>receive messages and write them to a file.</li>
 * <li>visualize incoming messages using Swing or JavaFX.</li>
 * </ul>
 * 
 * @author Kasra Faghihi
 */
public interface Gateway extends AutoCloseable {

    /**
     * Get the shuttle used to receive messages.
     * @return shuttle for incoming messages
     */
    Shuttle getIncomingShuttle();
    
    /**
     * Queue an outgoing shuttle to be added. When this gateway sends a message, that message will be forwarded to the appropriate outgoing
     * shuttles.
     * <p>
     * Note that this operation queues a shuttle to be added rather than adding it right away. As such, this method will likely
     * return before the add operation completes, and any error encountered during the operation will not be known to the caller. On error,
     * this gateway terminates.
     * <p>
     * If this gateway has been shutdown prior to calling this method, this method does nothing.
     * <p>
     * @param shuttle outgoing shuttle to add
     * @throws NullPointerException if any argument is {@code null}
     */
    void addOutgoingShuttle(Shuttle shuttle);

    /**
     * Queue an outgoing shuttle for removal.
     * <p>
     * Note that this operation queues a shuttle to be added rather than adding it right away. As such, this method will likely
     * return before the add operation completes, and any error encountered during the operation will not be known to the caller.
     * <p>
     * If this gateway has been shutdown prior to calling this method, this method does nothing. On error, this {@link Gateway}
     * terminates.
     * <p>
     * @param shuttlePrefix address prefix for shuttle to remove
     * @throws NullPointerException if any argument is {@code null}
     */
    void removeOutgoingShuttle(String shuttlePrefix);
}
