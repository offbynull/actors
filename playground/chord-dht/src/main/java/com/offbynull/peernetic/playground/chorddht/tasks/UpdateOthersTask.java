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
            int maxIdx = ChordUtils.getBitLength(context.getSelfId());
            for (int i = 0; i < maxIdx; i++) {
                Id routerId = context.getFingerTable().getRouterId(i);
                
                Pointer pointer = context.getFingerTable().findClosestPreceding(routerId);
                if (pointer instanceof InternalPointer) {
                    continue;
                }
                
                ExternalPointer<A> externalPointer = (ExternalPointer<A>) pointer;

                RouteToPredecessorTask<A> routeToPredTask = RouteToPredecessorTask.createAndAssignToRouter(getTime(), context,
                        externalPointer, routerId);
                waitUntilFinished(routeToPredTask.getEncapsulatingActor());

                ExternalPointer<A> foundRouter = routeToPredTask.getResult();
                if (foundRouter != null) {
                    sendAndWaitUntilResponse(new UpdateFingerTableRequest(routerId.getValueAsByteArray(), i), foundRouter.getAddress(),
                            UpdateFingerTableResponse.class);
                }
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
