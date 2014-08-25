package com.offbynull.peernetic.playground.unstructuredmesh;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import java.time.Instant;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.Validate;

public final class ContinuationActor<A> implements Actor {
    private UnstructuredClient<A> runnable;
    private Continuation continuation;

    public ContinuationActor(UnstructuredClient<A> client) {
        Validate.notNull(client);
        runnable = client;
        continuation = Continuation.startSuspendedWith(runnable);
    }

    @Override
    public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        runnable.setMessage(message);
        runnable.setSource(source);
        runnable.setTime(time);

        continuation = Continuation.continueWith(continuation);
    }

    
}
