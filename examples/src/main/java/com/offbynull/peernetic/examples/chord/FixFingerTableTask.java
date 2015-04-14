package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.ParentCoroutine;
import java.time.Duration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FixFingerTableTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(FixFingerTableTask.class);

    private final String sourceId;
    private final State state;

    public FixFingerTableTask(String sourceId, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        
        Context ctx = (Context) cnt.getContext();
        ParentCoroutine parentCoroutine = new ParentCoroutine(sourceId, state.getTimerPrefix(), ctx);
        
        int len = state.getFingerTableLength();

        while (true) {
            for (int i = 1; i < len; i++) { // this task starts from 1, not 0 ... fixing of the 0 / successor is done in stabilize
                LOG.info("{}: Fixing finger {}", state.getSelfId(), i);
                fixFinger(cnt, parentCoroutine, 0);
                fixFinger(cnt, parentCoroutine, i);
            }

            parentCoroutine.addSleep(Duration.ofSeconds(1L));
            parentCoroutine.run(cnt);
        }
    }
    
    private void fixFinger(Continuation cnt, ParentCoroutine parentCoroutine, int i) throws Exception {
        // get expected id of entry in finger table
        NodeId findId = state.getExpectedFingerId(i);

        // route to id
        Pointer foundFinger;
        try {
            String idSuffix = "" + state.generateExternalMessageId();
            RouteToTask routeToTask = new RouteToTask(
                    AddressUtils.parentize(sourceId, idSuffix),
                    state,
                    findId);
            parentCoroutine.add(idSuffix, routeToTask);
            parentCoroutine.run(cnt);
            foundFinger = routeToTask.getResult();
        } catch (RuntimeException re) {
            LOG.warn("Unable to find finger for index {}", i);
            return;
        }

        if (foundFinger instanceof InternalPointer) {
            // get existing finger in that slot... if it's not self, remove it... removing it should automatically shift the next finger in
            // to its place
            Pointer existingFinger = state.getFinger(i);
            if (existingFinger instanceof ExternalPointer) {
                state.removeFinger((ExternalPointer) existingFinger);
            }
        } else if (foundFinger instanceof ExternalPointer) {
            state.putFinger((ExternalPointer) foundFinger);
            LOG.debug("{}: Finger for index {} set to {}", state.getSelfId(), i, foundFinger);
        } else {
            throw new IllegalStateException();
        }
    }
}
