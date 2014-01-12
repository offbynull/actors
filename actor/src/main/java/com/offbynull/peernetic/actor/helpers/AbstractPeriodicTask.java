/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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

import org.apache.commons.lang3.Validate;

/**
 * A task that's executed periodically.
 * @author Kasra Faghihi
 */
public abstract class AbstractPeriodicTask extends AbstractChainedTask {

    private Stage stage = Stage.START;
    private long waitDuration;

    /**
     * Constructs a {@link AbstractPeriodicTask} object.
     * @param waitDuration amount of time to wait before executing the underlying task
     * @throws IllegalArgumentException if any numeric argument is negative
     */
    public AbstractPeriodicTask(long waitDuration) {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, waitDuration);
        this.waitDuration = waitDuration;
    }
    
    @Override
    protected final Task switchTask(long timestamp, Task prev) {
        switch (stage) {
            case START:
            case PROCESSING: {
                stage = Stage.WAIT;
                return new WaitTask(timestamp + waitDuration);
            }
            case WAIT: {
                stage = Stage.PROCESSING;
                
                return startTask();
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    /**
     * Called when the periodically running task should be started.
     * @return new task to run
     */
    protected abstract Task startTask();
    
    private enum Stage {
        START,
        WAIT,
        PROCESSING
    }
}
