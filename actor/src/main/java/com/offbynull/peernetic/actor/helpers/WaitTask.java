/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.helpers;

import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import org.apache.commons.lang3.Validate;

/**
 * A task that waits for a some pre-determined amount of time.
 * @author Kasra Faghihi
 */
public final class WaitTask implements Task {

    private long hitTime;
    private TaskState state = TaskState.START;

    /**
     * Constructs a {@link WaitTask} object.
     * @param hitTime time to wait until
     * @throws IllegalArgumentException if any numeric argument is negative
     */
    public WaitTask(long hitTime) {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, hitTime);
        this.hitTime = hitTime;
    }
    
    @Override
    public TaskState getState() {
        return state;
    }

    @Override
    public long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (state) {
            case START: {
                if (timestamp >= hitTime) {
                    state = TaskState.COMPLETED;
                    return Long.MAX_VALUE;
                }
                
                state = TaskState.PROCESSING;
                return hitTime;
            }
            case PROCESSING: {
                if (timestamp >= hitTime) {
                    state = TaskState.COMPLETED;
                    return Long.MAX_VALUE;
                }
                
                return hitTime;
            }
            case COMPLETED: {
                return Long.MAX_VALUE;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
}
