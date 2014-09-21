package com.offbynull.peernetic.javaflow;

import com.offbynull.peernetic.actor.Endpoint;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class TaskState {
    private Instant time;
    private Endpoint source;
    private Object message;
    
    TaskState() {
    }

    void setTime(Instant time) {
        Validate.notNull(time);
        if (this.time != null) {
            Validate.isTrue(!time.isBefore(this.time)); // make sure we didn't go back in time
        }
        this.time = time;
    }

    void setSource(Endpoint source) {
        Validate.notNull(source);
        this.source = source;
    }

    void setMessage(Object message) {
        Validate.notNull(message);
        this.message = message;
    }

    public Instant getTime() {
        return time;
    }

    public Endpoint getSource() {
        return source;
    }

    public Object getMessage() {
        return message;
    }
    
}
