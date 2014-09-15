package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.BaseContinuableTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.ContinuationActor;
import com.offbynull.peernetic.playground.chorddht.messages.external.UpdateFingerTableRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.UpdateFingerTableResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;

public final class UpdateOthersTask<A> extends BaseContinuableTask<A, byte[]> {

    private final ChordContext<A> context;

    public static <A> UpdateOthersTask<A> createAndAssignToRouter(Instant time, ChordContext<A> context) throws Exception {
        // create
        UpdateOthersTask<A> task = new UpdateOthersTask<>(context);
        ContinuationActor encapsulatingActor = new ContinuationActor(task);
        task.setEncapsulatingActor(encapsulatingActor);

        // register types here
        // send priming message
        encapsulatingActor.onStep(time, NullEndpoint.INSTANCE, new InternalStart());

        return task;
    }

    public static <A> void unassignFromRouter(ChordContext<A> context, UpdateOthersTask<A> task) {
        context.getRouter().removeActor(task.getEncapsulatingActor());
    }

    private UpdateOthersTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        this.context = context;
    }

    @Override
    public void run() {
        try {
            // start timer
            scheduleTimer();
            
            while (true) {
                long uniqueExtPtrCount = context.getFingerTable().dump().stream()
                        .distinct()
                        .filter(x -> x instanceof ExternalPointer)
                        .count();
                if (uniqueExtPtrCount == 0L) {
                    throw new IllegalStateException();
                } else if (uniqueExtPtrCount == 1L) {
                    // special case not handled in chord paper's pseudo code
                    //
                    // if connecting to a overlay of size 1, find_predecessor() will always return yourself, so the node in the overlay will
                    // never get your request to update its finger table and you will never be recognized
                    ExternalPointer<A> ptr = (ExternalPointer<A>) context.getFingerTable().get(0);
                    sendAndWaitUntilResponse(new UpdateFingerTableRequest(context.getSelfId().getValueAsByteArray()),
                            ptr.getAddress(), UpdateFingerTableResponse.class);
                } else {            
                    int maxIdx = ChordUtils.getBitLength(context.getSelfId());
                    for (int i = 0; i < maxIdx; i++) {
                        // get id of node that should have us in its finger table at index i
                        Id routerId = context.getFingerTable().getRouterId(i);

                        // route to that id or the closest predecessor of that id
                        RouteToPredecessorTask<A> routeToPredTask =
                                RouteToPredecessorTask.createAndAssignToRouter(getTime(), context, routerId);
                        waitUntilFinished(routeToPredTask.getEncapsulatingActor());
                        Pointer foundRouter = routeToPredTask.getResult();

                        // if found, let it know that we think it might need to have use in its finger table
                        if (foundRouter != null && foundRouter instanceof ExternalPointer) {
                            sendAndWaitUntilResponse(new UpdateFingerTableRequest(context.getSelfId().getValueAsByteArray()),
                                    ((ExternalPointer<A>) foundRouter).getAddress(), UpdateFingerTableResponse.class);
                        }
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
