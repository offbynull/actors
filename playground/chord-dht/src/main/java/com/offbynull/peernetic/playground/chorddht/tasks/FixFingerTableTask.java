package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;

public final class FixFingerTableTask<A> extends ChordTask<A> {

    public static <A> FixFingerTableTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        FixFingerTableTask<A> task = new FixFingerTableTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private FixFingerTableTask(ChordContext<A> context) {
        super(context);
    }

    @Override
    public void execute() throws Exception {
        int maxIdx = ChordUtils.getBitLength(getContext().getSelfId());

        while (true) {
            for (int i = 0; i < maxIdx; i++) {
                // get expected id of entry in finger table
                Id findId = getContext().getFingerTable().getExpectedId(i);

                // route to id
                RouteToTask<A> routeToFingerTask = RouteToTask.create(getTime(), getContext(), findId);
                getFlowControl().waitUntilFinished(routeToFingerTask.getActor(), Duration.ofSeconds(1L));
                Pointer foundFinger = routeToFingerTask.getResult();

                // set in to finger table
                if (foundFinger == null) {
                    continue;
                }

                if (foundFinger instanceof InternalPointer) {
//                        Pointer existingPointer = getContext().getFingerTable().get(i);
//                        if (existingPointer instanceof ExternalPointer) {
//                            getContext().getFingerTable().remove((ExternalPointer<A>) existingPointer);
//                        }
                } else if (foundFinger instanceof ExternalPointer) {
                    getContext().getFingerTable().put((ExternalPointer<A>) foundFinger);
                } else {
                    throw new IllegalStateException();
                }

                // update successor table (if first)
                if (i == 0) {
                    getContext().getSuccessorTable().updateTrim(foundFinger);
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
