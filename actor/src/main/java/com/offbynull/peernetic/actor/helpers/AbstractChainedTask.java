package com.offbynull.peernetic.actor.helpers;

import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;

public abstract class AbstractChainedTask implements Task {

    private Task currentTask;
    private TaskState state = TaskState.START;

    @Override
    public final long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (state) {
            case START:
                currentTask = switchTask(null);
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
                    currentTask = switchTask(currentTask);
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

    protected abstract Task switchTask(Task prev);

    protected final void setFinished(boolean failed) {
        if (failed) {
            state = TaskState.FAILED;
        } else {
            state = TaskState.COMPLETED;
        }
    }
}
