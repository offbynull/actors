package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.javaflow.JavaflowTask;
import java.time.Instant;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.Validate;

public final class JavaflowActor implements Actor {
    private JavaflowTask task;
    private Continuation continuation;

    public JavaflowActor(JavaflowTask task) {
        Validate.notNull(task);
        this.task = task;
        continuation = Continuation.startSuspendedWith(task);
    }

    @Override
    public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        // if continuation has ended, ignore any further messages
        if (continuation != null) {
            task.setMessage(message);
            task.setSource(source);
            task.setTime(time);
        
            continuation = Continuation.continueWith(continuation);
        }
    }

    public boolean isFinished() {
        return continuation == null;
    }
    
}
