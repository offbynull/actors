package com.offbynull.peernetic.javaflow;

import com.offbynull.peernetic.actor.Endpoint;
import java.time.Instant;

public abstract class BaseJavaflowTask implements JavaflowTask {

    private TaskState state;

    public BaseJavaflowTask() {
        state = new TaskState();
    }

    @Override
    public final void setTime(Instant time) {
        state.setTime(time);
    }

    @Override
    public final void setSource(Endpoint source) {
        state.setSource(source);
    }

    @Override
    public final void setMessage(Object message) {
        state.setMessage(message);
    }

    public final Instant getTime() {
        return state.getTime();
    }

    public final Endpoint getSource() {
        return state.getSource();
    }

    public final Object getMessage() {
        return state.getMessage();
    }

    @Override
    public TaskState getState() {
        return state;
    }
    
}
