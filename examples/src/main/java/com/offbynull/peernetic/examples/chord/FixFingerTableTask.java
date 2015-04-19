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
        
        LOG.debug("{} {} - Starting fix finger task", state.getSelfId(), sourceId);
        
        int len = state.getFingerTableLength();

        while (true) {
            for (int i = 0; i < len; i++) {
                // Fix successor (finger idx 0) and the finger at i. If i == 0, this will fix the successor twice, which is not really
                // required but it doesn't hurt.
                
                LOG.debug("{} {} - Fixing finger 0", state.getSelfId(), sourceId);
                fixFinger(cnt, 0);
                LOG.debug("{} {} - Fixing finger {}", state.getSelfId(), sourceId, i);
                fixFinger(cnt, i);
            }

            LOG.debug("{} {} - Fingers after fix are {}", state.getSelfId(), sourceId, state.getFingers());
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
            LOG.error("{} {} - Error while finding index for {}", state.getSelfId(), sourceId, i);
            return;
        }
        
        if (foundFinger == null) {
            // Routing failed, so set found finger as 'self', which will cause the finger at this index to be removed in the if/else block
            // just after this
            foundFinger = new InternalPointer(state.getSelfId());
            LOG.debug("{} {} - Unable to find index for {}", state.getSelfId(), sourceId, i);
        } else {
            LOG.debug("{} {} - Finger for index {} set to {}", state.getSelfId(), sourceId, i, foundFinger);
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
        } else {
            throw new IllegalStateException();
        }
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        
        SleepCoroutine sleepCoroutine = new SleepCoroutine(sourceId, state.getTimerPrefix(), duration);
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
