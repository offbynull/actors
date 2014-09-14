package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;

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
                for (int i = 0; i < maxIdx; i++) {
                    // get expected id of entry in finger table
                    Id findId = context.getFingerTable().getExpectedId(i);

                    // route to id
                    RouteToSuccessorTask<A> routeToFingerTask = RouteToSuccessorTask.createAndAssignToRouter(getTime(), context, findId);
                    waitUntilFinished(routeToFingerTask.getEncapsulatingActor());
                    Pointer foundFinger = routeToFingerTask.getResult();
                    
                    // set in to finger table
                    if (foundFinger == null) {
                        continue;
                    }
                    
                    if (foundFinger instanceof InternalPointer) {
                        Pointer existingPointer = context.getFingerTable().get(i);
                        if (existingPointer instanceof ExternalPointer) {
                            context.getFingerTable().remove((ExternalPointer<A>) existingPointer);
                        }
                    } else if (foundFinger instanceof ExternalPointer) {
                        context.getFingerTable().put((ExternalPointer<A>) foundFinger);
                    } else {
                        throw new IllegalStateException();
                    }
                    
                    // update successor table (if first)
                    if (i == 0) {
                        context.getSuccessorTable().updateTrim(foundFinger);
                    }
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
