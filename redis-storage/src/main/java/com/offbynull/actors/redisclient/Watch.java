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
package com.offbynull.actors.redisclient;

import org.apache.commons.lang3.Validate;

/**
 * Redis WATCH operation.
 * @author Kasra Faghihi
 */
public final class Watch {

    private final String key;
    private final boolean retry;
    private final WatchBlock block;

    /**
     * Constructs a {@link Watch} object.
     * @param key key to watch
     * @param retry if {@code true}, watch condition will be retried until it passes
     * @param block block that defines watch conditions
     * @throws NullPointerException if any argument is {@code null}
     */
    public Watch(String key, boolean retry, WatchBlock block) {
        Validate.notNull(key);
        Validate.notNull(block);
        this.key = key;
        this.retry = retry;
        this.block = block;
    }

    /**
     * Get key.
     * @return key to watch. 
     */
    public String getKey() {
        return key;
    }

    /**
     * Get retry flag.
     * @return if {@code true}, watch condition will be retried until it passes
     */
    public boolean getRetry() {
        return retry;
    }

    /**
     * Get watch condition block.
     * @return block that defines watch conditions
     */
    public WatchBlock getBlock() {
        return block;
    }

}
