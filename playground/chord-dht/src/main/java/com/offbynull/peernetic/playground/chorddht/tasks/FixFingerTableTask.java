package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.skeleton.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.ChordOperationException;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.Pointer;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FixFingerTableTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private static final Logger LOG = LoggerFactory.getLogger(FixFingerTableTask.class);
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
        this.chordHelper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        int len = chordHelper.getFingerTableLength();

        while (true) {
            for (int i = 1; i < len; i++) { // this task starts from 1, not 0 ... fixing of the 0 / successor is done in stabilize
                LOG.info("{}: Fixing finger {}", chordHelper.getSelfId(), i);
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
            // get existing finger in that slot... if it's not self, remove it... removing it should automatically shift the next finger in
            // to its place
            Pointer existingFinger = chordHelper.getFinger(i);
            if (existingFinger instanceof ExternalPointer) {
                chordHelper.removeFinger((ExternalPointer<A>) existingFinger);
            }
        } else if (foundFinger instanceof ExternalPointer) {
            chordHelper.putFinger((ExternalPointer<A>) foundFinger);
            LOG.debug("{}: Finger for index {} set to {}", chordHelper.getSelfId(), i, foundFinger);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
