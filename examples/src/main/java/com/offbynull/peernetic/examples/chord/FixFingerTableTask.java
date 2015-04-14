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
import com.offbynull.peernetic.examples.common.coroutines.RequestCoroutine;
import com.offbynull.peernetic.examples.common.coroutines.SleepCoroutine;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
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
        
        int len = state.getFingerTableLength();

        while (true) {
            for (int i = 1; i < len; i++) { // this task starts from 1, not 0 ... fixing of the 0 / successor is done in stabilize
                LOG.info("{}: Fixing finger {}", state.getSelfId(), i);
                fixFinger(cnt, 0);
                fixFinger(cnt, i);
            }

            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
        }
    }
    
    private void fixFinger(Continuation cnt, int i) throws Exception {
        // get expected id of entry in finger table
        NodeId findId = state.getExpectedFingerId(i);

        // route to id
        Pointer foundFinger;
        try {
            foundFinger = funnelToRouteToCoroutine(cnt, findId);
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
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        
        SleepCoroutine sleepCoroutine = new SleepCoroutine(state.getTimerPrefix(), duration);
        sleepCoroutine.run(cnt);
    }

    private Pointer funnelToRouteToCoroutine(Continuation cnt, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "" + state.generateExternalMessageId();
        RouteToTask innerCoroutine = new RouteToTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
}
