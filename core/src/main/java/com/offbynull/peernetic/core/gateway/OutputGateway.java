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
package com.offbynull.peernetic.core.gateway;

import com.offbynull.peernetic.core.shuttle.Shuttle;

/**
 * A {@link Gateway} that can dynamically to add and remove destinations where it sends outgoing messages. If you're only going to write
 * outoing messages to a known set of {@link Shuttle}s, you could implement a {@link Gateway} rather than an {@link OutputGateway}. Use
 * {@link OutputGateway} only when you need to <b>dynamically add and remove</b> destinations for outgoing messages.
 * @author Kasra Faghihi
 */
public interface OutputGateway extends Gateway {
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
