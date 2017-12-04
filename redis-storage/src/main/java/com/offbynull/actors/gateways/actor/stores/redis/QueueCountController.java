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
package com.offbynull.actors.gateways.actor.stores.redis;

import org.apache.commons.lang3.Validate;

/**
 * Controls the number of timestamp queues. This count can be modified even after being passed into {@link RedisStore}.
 * <p>
 * Increase to spread load over multiple queues in times of high-load, then decrease to reduce the number of queues back down.
 * @author Kasra Faghihi
 */
public final class QueueCountController {

    private volatile int count;

    /**
     * Constructs a {@link QueueCountController} object.
     * @param count initial number of queues
     * @throws IllegalArgumentException if {@code count <= 0}
     */
    public QueueCountController(int count) {
        Validate.isTrue(count >= 1);
        this.count = count;
    }

    /**
     * Get queue count.
     * @return queue count
     */
    public int getCount() {
        return count;
    }

    /**
     * Set queue count.
     * @param count queue count
     * @throws IllegalArgumentException if {@code count <= 0}
     */
    public void setCount(int count) {
        Validate.isTrue(count >= 1);
        this.count = count;
    }
    
}
