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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Results for each operation performed on a {@link TransactionQueue}.
 * @author Kasra Faghihi
 */
public final class TransactionResult {
    private final List<Object> result;

    /**
     * Constructs a {@link TransactionResult} object.
     * @param result results (can contain elements that are {@code null})
     * @throws NullPointerException if any argument is {@code null}
     */
    public TransactionResult(List<Object> result) {
        Validate.notNull(result);
        this.result = new ArrayList<>(result);
    }
    
    /**
     * Get result for a queued command.
     * @param <T> expected time
     * @param idx index of command
     * @throws IllegalArgumentException if {@code idx < 0 || idx >= size()}
     * @return result result for index at command {@code idx}
     */
    public <T> T get(int idx) {
        Validate.isTrue(idx >= 0 && idx < result.size());
        return (T) result.get(idx);
    }

    /**
     * Get number of results.
     * @return number of results
     */
    public int size() {
        return result.size();
    }
}
