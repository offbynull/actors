package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.javaflow.FlowControl;
import com.offbynull.peernetic.javaflow.BaseJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public abstract class ChordTask<A> extends BaseJavaflowTask {
    private JavaflowActor actor;
    private final ChordContext<A> context;
    private final FlowControl<A, byte[]> flowControl;

    public ChordTask(ChordContext<A> context) {
        Validate.notNull(context);
        this.context = context;
        flowControl = new FlowControl<>(getState(), context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(),
                context.getNonceAccessor());
    }
    
    public final void initialize(Instant time, JavaflowActor actor) throws Exception {
        Validate.validState(this.actor == null);
        Validate.notNull(actor);
        Validate.notNull(time);
        
        this.actor = actor;
        flowControl.initialize(actor);
        
        if (requiresPriming()) {
            actor.onStep(time, NullEndpoint.INSTANCE, new Object());
        }
    }
    
    @Override
    public final void run() {
        Validate.validState(actor != null);
        try {
            execute();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            getContext().getRouter().removeActor(actor);
        }
    }
    
    public final ChordContext<A> getContext() {
        Validate.validState(actor != null);
        return context;
    }
    
    public final JavaflowActor getActor() {
        Validate.validState(actor != null);
        return actor;
    }

    public FlowControl<A, byte[]> getFlowControl() {
        return flowControl;
    }
    
    protected abstract boolean requiresPriming();
    
    protected abstract void execute() throws Exception;
    
    
}
