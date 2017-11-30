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
package com.offbynull.actors.stores.redis.connector;

/**
 * Redis operations to perform inside a MULTI/EXEC block.
 * @author Kasra Faghihi
 */
public interface TransactionBlock {
    /**
     * Queues Redis operations to perform in a MULTI/EXEC block.
     * @param queue transaction queue
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if connector closed
     */
    void execute(TransactionQueue queue) throws ConnectionException; 
}
