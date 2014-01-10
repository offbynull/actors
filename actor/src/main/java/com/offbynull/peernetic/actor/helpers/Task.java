package com.offbynull.peernetic.actor.helpers;

import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;

public interface Task {
    
    TaskState getState();
    long process(long timestamp, Incoming incoming, PushQueue pushQueue);
        
    public enum TaskState {
        START,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
