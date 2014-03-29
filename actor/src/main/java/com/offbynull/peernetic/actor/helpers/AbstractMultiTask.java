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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Groups one or more {@link Task}s at together such that they're all run at the same time.
 * @author Kasra Faghihi
 */
public abstract class AbstractMultiTask implements Task {

    private Set<Task> currentTasks;
    private TaskState state = TaskState.START;

    @Override
    public final long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (state) {
            case START:
                currentTasks = taskStateUpdated(Collections.<Task>emptySet(), timestamp, pushQueue);
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
        
        Set<Long> nextTimestamps = new HashSet<>();
        Set<Task> finishedTasks = new HashSet<>();
        iterateTasks(timestamp, incoming, pushQueue, currentTasks, finishedTasks, nextTimestamps);

        while (!finishedTasks.isEmpty()) {
            Set<Task> newTasks = taskStateUpdated(finishedTasks, timestamp, pushQueue); // notify finished tasks and get new tasks
            
            if (state == TaskState.COMPLETED || state == TaskState.FAILED) {
                break;
            }
            
            currentTasks.addAll(newTasks);

            finishedTasks.clear();
            iterateTasks(timestamp, incoming, pushQueue, currentTasks, finishedTasks, nextTimestamps); // iterate once for all new tags
        }
        
        switch (state) { // ensure not marked as finished
            case COMPLETED:
            case FAILED:
                return Long.MAX_VALUE;
            case PROCESSING:
                return nextTimestamps.isEmpty() ? Long.MAX_VALUE : Collections.min(nextTimestamps);
            default:
                throw new IllegalStateException();
        }
    }
    
    private void iterateTasks(long timestamp, Incoming incoming, PushQueue pushQueue,
            Set<Task> tasks, Set<Task> finishedTasks, Set<Long> nextTimestamps) {
        
        for (Task currentTask : tasks) {
            long nextTimestamp = currentTask.process(timestamp, incoming, pushQueue);
            nextTimestamps.add(nextTimestamp);

            switch (currentTask.getState()) {
                case START:
                case PROCESSING:
                    break;
                case COMPLETED:
                case FAILED:
                    finishedTasks.add(currentTask);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override
    public final TaskState getState() {
        return state;
    }

    /**
     * Called when one or more sub-tasks have completed/failed, and when this task first starts.
     * @param finished finished sub-tasks -- empty if this task was just started
     * @param timestamp current time
     * @param pushQueue push queue
     * @return next {@code Task}s to run
     */
    protected abstract Set<Task> taskStateUpdated(Set<Task> finished, long timestamp, PushQueue pushQueue);

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
