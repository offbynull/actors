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

/**
 * A task. Performs some functionality and has a state: described in {@link TaskState}.
 * @author Kasra Faghihi
 */
public interface Task {
    
    /**
     * Get the task state.
     * @return task state
     */
    TaskState getState();
    
    /**
     * Process a task.
     * @param timestamp current timestamp
     * @param incoming incoming message (if any -- can be {@code null})
     * @param pushQueue push queue for outgoing messages
     * @return latest time that this method should be called again
     * @throws NullPointerException if any argument other than {@code incoming} is {@code null}
     */
    long process(long timestamp, Incoming incoming, PushQueue pushQueue);
    
    /**
     * Task state.
     */
    public enum TaskState {
        /**
         * Initial state upon creating the task.
         */
        START,
        /**
         * Task is currently working.
         */
        PROCESSING,
        /**
         * Task has completed.
         */
        COMPLETED,
        /**
         * Task has failed.
         */
        FAILED
    }
}
