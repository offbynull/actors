package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import org.apache.commons.javaflow.Continuation;

public final class FixFingerTableTask<A> extends BaseContinuableTask<A, byte[]> {

    private final ChordContext<A> context;

    public static <A> FixFingerTableTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context) throws Exception {
        // create
        FixFingerTableTask<A> task = new FixFingerTableTask<>(context);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);

        // register types here

        // send priming message
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());
        
        return task;
    }

    public static <A> void unassignFromRouter(ChordContext<A> context, FixFingerTableTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }

    private FixFingerTableTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        this.context = context;
    }

    @Override
    public void run() {
        try {
            int maxIdx = ChordUtils.getBitLength(context.getSelfId());
            
            // start timer
            scheduleTimer();
            
            while (true) {
                for (int i = 1; i <= maxIdx; i++) {
                    Id findId = context.getFingerTable().getExpectedId(i);
                    Pointer fromNode = context.getFingerTable().get(0);
                    
                    if (!(fromNode instanceof ExternalPointer)) {
                        break;
                    }

                    RouteToFingerTask<A> routeToFingerTask = RouteToFingerTask.createAndAssignToRouter(getTime(), context,
                            ((ExternalPointer<A>) fromNode), findId);
                    waitUntilFinished(routeToFingerTask.getEncapsulatingActor());

                    ExternalPointer<A> foundFinger = routeToFingerTask.getResult();
                    context.getFingerTable().put(foundFinger);
                }
                
                waitCycles(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            unassignFromRouter(context, this);
        }
    }
    
    private static final class InternalStart {
        
    }
}
