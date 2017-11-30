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

import org.apache.commons.lang3.Validate;

/**
 * Redis MULTI/EXEC transaction operation.
 * @author Kasra Faghihi
 */
public final class Transaction {

    private final TransactionBlock block;
    private final boolean retry;

    /**
     * Constructs a {@link Transaction} object.
     * @param retry if {@code true}, transaction will be retried until it passes
     * @param block block that defines queued transaction operations
     * @throws NullPointerException if any argument is {@code null}
     */
    public Transaction(boolean retry, TransactionBlock block) {
        Validate.notNull(block);
        this.retry = retry;
        this.block = block;
    }

    /**
     * Get retry flag.
     * @return if {@code true}, transaction will be retried until it passes
     */
    public boolean isRetry() {
        return retry;
    }

    /**
     * Get transaction block.
     * @return block that defines queued transaction operations
     */
    public TransactionBlock getBlock() {
        return block;
    }

}
