package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class UpdateOthersTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private final ChordContext<A> context;
    private final ChordHelper<A, byte[]> chordHelper;
    
    public static <A> UpdateOthersTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        UpdateOthersTask<A> task = new UpdateOthersTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private UpdateOthersTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.context = context;
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
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
                chordHelper.sendUpdateFingerTableRequest(ptr.getAddress(), context.getSelfId());
            } else {
                int maxIdx = ChordUtils.getBitLength(context.getSelfId());
                for (int i = 0; i < maxIdx; i++) {
                    // get id of node that should have us in its finger table at index i
                    Id routerId = context.getFingerTable().getRouterId(i);

                    // route to that id or the closest predecessor of that id
                    RouteToPredecessorTask<A> routeToPredTask
                            = RouteToPredecessorTask.create(getTime(), context, routerId);
                    getFlowControl().waitUntilFinished(routeToPredTask.getActor(), Duration.ofSeconds(1L));
                    Pointer foundRouter = routeToPredTask.getResult();

                    // if found, let it know that we think it might need to have use in its finger table
                    if (foundRouter != null && foundRouter instanceof ExternalPointer) {
                        chordHelper.sendUpdateFingerTableRequest(((ExternalPointer<A>) foundRouter).getAddress(), context.getSelfId());
                    }
                }
            }

            getFlowControl().wait(Duration.ofSeconds(1L));
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
