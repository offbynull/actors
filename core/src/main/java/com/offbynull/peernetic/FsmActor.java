package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import java.time.Instant;
import java.util.function.Function;
import org.apache.commons.lang3.Validate;

public final class FsmActor<C> implements Actor {
    private final FiniteStateMachine<C> fsm;
    private final Function<Endpoint, C> contextProducer;

    public static FsmActor<Endpoint> create(Object object, String initialState) {
        return new FsmActor<Endpoint>(object, initialState, Endpoint.class, (x) -> x);
    }

    public FsmActor(Object object, String initialState, Class<C> contextType, C context) {
        this(object, initialState, contextType, (x) -> context);
    }

    public FsmActor(Object object, String initialState, Class<C> contextType, Function<Endpoint, C> contextProducer) {
        Validate.notNull(object);
        Validate.notNull(initialState);
        Validate.notNull(contextType);
        Validate.notNull(contextProducer);
        
        this.fsm = new FiniteStateMachine<>(object, initialState, contextType);
        this.contextProducer = contextProducer;
    }

    @Override
    public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        fsm.process(time, message, contextProducer.apply(source));
    }
}
