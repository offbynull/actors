package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import java.time.Instant;
import org.apache.commons.javaflow.Continuation;

public final class CheckPredecessorTask<A> extends BaseContinuableTask<A, byte[]> {

    private final ChordContext<A> context;

    public static <A> CheckPredecessorTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context) throws Exception {
        // create
        CheckPredecessorTask<A> task = new CheckPredecessorTask<>(context);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);

        // register types here

        // send priming message
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }

    public static <A> void unassignFromRouter(ChordContext<A> context, CheckPredecessorTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }

    private CheckPredecessorTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        this.context = context;
    }

    @Override
    public void run() {
        try {
            // start timer
            scheduleTimer();
            
            while (true) {
                ExternalPointer<A> predecessor = context.getPredecessor();
                
                if (predecessor == null) {
                    waitCycles(1);
                    continue;
                }
                GetIdResponse gir = sendAndWaitUntilResponse(new GetIdRequest(), predecessor.getAddress(), GetIdResponse.class);
                Id id = new Id(gir.getId(), context.getSelfId().getLimitAsByteArray());
                
                if (!id.equals(predecessor.getId())) {
                    context.setPredecessor(null);
                }
                
                waitCycles(1);
            }
        } finally {
            unassignFromRouter(context, this);
        }
    }
    
    private static final class InternalStart {
        
    }
}