/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.stores.redis.client;

import com.offbynull.actors.shuttle.Address;
import java.io.Closeable;

/**
 * Redis client.
 * <p>
 * Implementations may be bound to a {@link ClientFactory}. That means that if the {@link ClientFactory} that generated this {@link Client}
 * is closed, this {@link Client} may also be closed.
 * <p>
 * Implementations may or may not be thread-safe.
 * @author Kasra Faghihi
 */
public interface Client extends Closeable {

    /**
     * Get actor accessor.
     * @param address address for actor
     * @return actor accessor
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    ActorAccessor getActorAccessor(Address address);
    
    /**
     * Get message check queue.
     * @param num queue number
     * @return message check queue
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    TimestampQueue getMessageCheckQueue(int num);
    
    /**
     * Get checkpoint check queue.
     * @param num queue number
     * @return checkpoint check queue
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if closed
     */
    TimestampQueue getCheckpointCheckQueue(int num);
}
