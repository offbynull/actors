package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.ChordOperationException;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FixFingerTableTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private static final Logger LOG = LoggerFactory.getLogger(FixFingerTableTask.class);
    private final ChordContext<A> context;
    private final ChordHelper<A, byte[]> chordHelper;
    
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
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        int len = chordHelper.getFingerTableLength();

        while (true) {
            for (int i = 1; i < len; i++) {
                // for each finger to be fixed, fix the successor (finger[0]) befor fixing that finger. if we're not aggressive about fixing
                // successor, ring may never be complete
                fixFinger(0);
                fixFinger(i);
            }

            chordHelper.sleep(1L);
        }
    }
    
    private void fixFinger(int i) throws Exception {
        // get expected id of entry in finger table
        Id findId = chordHelper.getExpectedFingerId(i);

        // route to id
        Pointer foundFinger;
        try {
            foundFinger = chordHelper.runRouteToTask(findId);
        } catch (ChordOperationException coe) {
            LOG.warn("Unable to find finger for index {}", i);
            return;
        }

        if (foundFinger instanceof InternalPointer) {
//                        Pointer existingPointer = context.getFingerTable().get(i);
//                        if (existingPointer instanceof ExternalPointer) {
//                            context.getFingerTable().remove((ExternalPointer<A>) existingPointer);
//                        }
        } else if (foundFinger instanceof ExternalPointer) {
            chordHelper.putFinger((ExternalPointer<A>) foundFinger);
            LOG.debug("{}: Finger for index {} set to {}", context.getSelfId(), i, foundFinger);
        } else {
            throw new IllegalStateException();
        }

        // update successor table (if first)
        if (i == 0) {
            chordHelper.setImmediateSuccessor(foundFinger);
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
