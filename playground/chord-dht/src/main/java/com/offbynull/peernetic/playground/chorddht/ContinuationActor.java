package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.playground.chorddht.tasks.GenerateFingerTableTask;
import java.time.Instant;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.Validate;

public final class ContinuationActor implements Actor {
    private ContinuableTask runnable;
    private Continuation continuation;

    public ContinuationActor(ContinuableTask client) {
        Validate.notNull(client);
        runnable = client;
        continuation = Continuation.startSuspendedWith(runnable);
    }

    @Override
    public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        // if continuation has ended, ignore any further messages
        if (continuation != null) {
            runnable.setMessage(message);
            runnable.setSource(source);
            runnable.setTime(time);
        
            continuation = Continuation.continueWith(continuation);
            if (runnable.getClass() == GenerateFingerTableTask.class && continuation == null) {
                System.out.println("hi");
            }
        }
    }

    public boolean isFinished() {
        return continuation == null;
    }
    
}
