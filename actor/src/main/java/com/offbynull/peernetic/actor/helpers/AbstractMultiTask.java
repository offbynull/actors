package com.offbynull.peernetic.actor.helpers;

import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractMultiTask implements Task {

    private Set<Task> currentTasks;
    private TaskState state = TaskState.START;

    @Override
    public final long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (state) {
            case START:
                currentTasks = taskStateUpdated(Collections.<Task>emptySet());
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
            Set<Task> newTasks = taskStateUpdated(finishedTasks); // notify finished tasks and get new tasks
            
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

    protected abstract Set<Task> taskStateUpdated(Set<Task> finished);

    protected final void setFinished(boolean failed) {
        if (failed) {
            state = TaskState.FAILED;
        } else {
            state = TaskState.COMPLETED;
        }
    }
}
