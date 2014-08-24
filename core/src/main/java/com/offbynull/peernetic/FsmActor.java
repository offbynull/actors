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
    private C context;

    public static FsmActor<Endpoint> create(Object object, String initialState) {
        return new FsmActor<>(object, initialState, Endpoint.class, null, (src) -> src);
    }

    public FsmActor(Object object, String initialState, Class<C> contextType, C context) {
        this(object, initialState, contextType, context, (x) -> context);
    }
    
    public FsmActor(Object object, String initialState, Class<C> contextType, C context, Function<Endpoint, C> contextProducer) {
        Validate.notNull(object);
        Validate.notNull(initialState);
        Validate.notNull(contextType);
//        Validate.notNull(context); // can be null.. for example, if we want the endpoint being passed in to be the func, (x) -> x
                                     // otherwise we can set the endpoint in the context, (x) -> { context.setSource(x); return context; }
                                     // or possibly create a new context by doing (x) -> { context.withSource(x); }
                                     //
                                     // x = src endpoint
        Validate.notNull(contextProducer);
        
        this.fsm = new FiniteStateMachine<>(object, initialState, contextType);
        this.contextProducer = contextProducer;
    }

    @Override
    public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        context = contextProducer.apply(source);
        fsm.process(time, message, context);
    }
}
