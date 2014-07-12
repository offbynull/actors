package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class FsmActor implements Actor {
    private final FiniteStateMachine<Endpoint> fsm;

    public FsmActor(Object object, String initialState) {
        Validate.notNull(object);
        Validate.notNull(initialState);
        
        this.fsm = new FiniteStateMachine<>(object, initialState, Endpoint.class);
    }


    @Override
    public void onStart(Instant time) throws Exception {
        // do nothing
    }

    @Override
    public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        fsm.process(time, message, source);
    }

    @Override
    public void onStop(Instant time) throws Exception {
        // do nothing
    }
}
