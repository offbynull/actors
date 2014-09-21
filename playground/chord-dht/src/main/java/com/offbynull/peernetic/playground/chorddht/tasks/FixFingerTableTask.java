package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.shared.ChordUtils;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class FixFingerTableTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private final ChordContext<A> context;
    
    public static <A> FixFingerTableTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        // create
        FixFingerTableTask<A> task = new FixFingerTableTask<>(context);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }

    private FixFingerTableTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.context = context;
    }

    @Override
    public void execute() throws Exception {
        int maxIdx = ChordUtils.getBitLength(context.getSelfId());

        while (true) {
            for (int i = 1; i < maxIdx; i++) {
                // for each finger to be fixed, fix the successor (finger[0]) befor fixing that finger. if we're not aggressive about fixing
                // successor, ring may never be complete
                fixFinger(0);
                fixFinger(i);
            }

            getFlowControl().wait(Duration.ofSeconds(1L));
        }
    }
    
    private void fixFinger(int i) throws Exception {
        // get expected id of entry in finger table
        Id findId = context.getFingerTable().getExpectedId(i);

        // route to id
        RouteToTask<A> routeToFingerTask = RouteToTask.create(getTime(), context, findId);
        getFlowControl().waitUntilFinished(routeToFingerTask.getActor(), Duration.ofSeconds(1L));
        Pointer foundFinger = routeToFingerTask.getResult();

        // set in to finger table
        if (foundFinger == null) {
            return;
        }

        if (foundFinger instanceof InternalPointer) {
//                        Pointer existingPointer = context.getFingerTable().get(i);
//                        if (existingPointer instanceof ExternalPointer) {
//                            context.getFingerTable().remove((ExternalPointer<A>) existingPointer);
//                        }
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

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
