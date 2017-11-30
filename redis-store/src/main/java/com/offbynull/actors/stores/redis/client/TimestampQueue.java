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

/**
 * Timestamp queue -- a queue of (timestamp, address) pairs that's sorted in ascending order by timestamp.
 * <p>
 * Unless there's a critical error, implementations are required to retry indefinity the operation until it succeeds. The following are not
 * considered to be critical errors...
 * <ul>
 * <li>Connection problems.</li>
 * <li>Redis MULTI/EXEC transactions that fail because a WATCH failed are not critical errors.</li>
 * </ul>
 * @author Kasra Faghihi
 */
public interface TimestampQueue {

    /**
     * Insert a new address into queue.
     * @param timestamp timestamp
     * @param address address
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code timestamp < 0}
     * @throws IllegalStateException if closed
     */
    void insert(long timestamp, Address address) throws ClientException;

    /**
     * Remove address at top of queue, so long as its timestamp is {@code <= minTimestamp}.
     * @param minTimestamp minimum timestamp required for a removal to occur
     * @return removed address
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code minTimestamp < 0}
     * @throws IllegalStateException if closed
     */
    Address remove(long minTimestamp) throws ClientException;
    
}
