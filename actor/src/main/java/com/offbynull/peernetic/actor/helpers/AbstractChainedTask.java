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
 * Chains one or more {@link Task}s together.
 * @author Kasra Faghihi
 */
public abstract class AbstractChainedTask implements Task {

    private Task currentTask;
    private TaskState state = TaskState.START;

    @Override
    public final long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (state) {
            case START:
                currentTask = switchTask(timestamp, null, pushQueue);
                if (state == TaskState.COMPLETED || state == TaskState.FAILED) { // if task switch marked this task as finished, stop
                    return Long.MAX_VALUE;
                }
                state = TaskState.PROCESSING;
                break;
            case COMPLETED:
            case FAILED:
                return Long.MAX_VALUE;
            default:
                break;
        }
        
        long nextTimestamp = Long.MAX_VALUE;
        
        top:
        while (true) {
            nextTimestamp = currentTask.process(timestamp, incoming, pushQueue);

            switch (currentTask.getState()) {
                case START:
                case PROCESSING:
                    break top;
                case COMPLETED:
                case FAILED:
                    currentTask = switchTask(timestamp, currentTask, pushQueue);
                    if (state == TaskState.COMPLETED || state == TaskState.FAILED) { // if task switch marked this task as finished, stop
                        break top;
                    }
                    break; // otherwise, run process once for the next task
                default:
                    throw new IllegalStateException();
            }
        }

        
        switch (state) { // ensure not marked as finished
            case COMPLETED:
            case FAILED:
                return Long.MAX_VALUE;
            case PROCESSING:
                return nextTimestamp;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public final TaskState getState() {
        return state;
    }

    /**
     * Called when the sub-task has completed/failed, and when this task first starts.
     * @param timestamp current time
     * @param prev current processing sub-task, or {@code null} if this task was just started
     * @param pushQueue push queue
     * @return next {@code Task}
     */
    protected abstract Task switchTask(long timestamp, Task prev, PushQueue pushQueue);

    /**
     * Sets this task's completion state.
     * @param failed {@code true} to set put this task in to a {@link TaskState#FAILED} state, {@code false} for {@link TaskState#COMPLETED}
     */
    protected final void setFinished(boolean failed) {
        if (failed) {
            state = TaskState.FAILED;
        } else {
            state = TaskState.COMPLETED;
        }
    }
}
