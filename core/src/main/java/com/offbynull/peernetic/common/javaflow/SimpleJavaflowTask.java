package com.offbynull.peernetic.common.javaflow;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.transmission.Router;
import com.offbynull.peernetic.javaflow.BaseJavaflowTask;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public abstract class SimpleJavaflowTask<A, N> extends BaseJavaflowTask {
    private JavaflowActor actor;
    private final Router<A, N> router;
    private final FlowControl<A, N> flowControl;

    public SimpleJavaflowTask(Router<A, N> router, Endpoint selfEndpoint, EndpointScheduler endpointScheduler,
            NonceAccessor<N> nonceAccessor) {
        this.router = router;
        flowControl = new FlowControl<>(getState(), router, selfEndpoint, endpointScheduler, nonceAccessor);
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
            router.removeActor(actor);
        }
    }
    
    public final JavaflowActor getActor() {
        Validate.validState(actor != null);
        return actor;
    }

    public FlowControl<A, N> getFlowControl() {
        Validate.validState(actor != null);
        return flowControl;
    }
    
    protected abstract boolean requiresPriming();
    
    protected abstract void execute() throws Exception;
    
    
}
