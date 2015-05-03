package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FixFingerTableTask implements Subcoroutine<Void> {
    
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
    public Void run(Continuation cnt) throws Exception {
        
        LOG.debug("{} {} - Starting fix finger task", state.getSelfId(), sourceId);
        
        int len = state.getFingerTableLength();

        while (true) {
            List<Pointer> oldFingers = state.getFingers();
            for (int i = 1; i < len; i++) {
                LOG.debug("{} {} - Fixing finger {}", state.getSelfId(), sourceId, i);
                fixFinger(cnt, i);
            }

            LOG.debug("{} {} - Fix finger cycle\nBefore: {}\nAfter: {}", state.getSelfId(), sourceId, oldFingers, state.getFingers());
            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
        }
    }
    
    private void fixFinger(Continuation cnt, int i) throws Exception {
        // get expected id of entry in finger table
        NodeId findId = state.getExpectedFingerId(i);

        // route to id
        Pointer foundFinger;
        try {
            foundFinger = funnelToRouteToSuccessorCoroutine(cnt, findId);
        } catch (RuntimeException re) {
            LOG.error("{} {} - Error while finding index for {}", state.getSelfId(), sourceId, i);
            return;
        }
        
        if (foundFinger == null) {
            // Routing failed, so set found finger as 'self', which will cause the finger at this index to be removed in the if/else block
            // just after this
            foundFinger = new InternalPointer(state.getSelfId());
        }
        
        
        Pointer oldFinger = state.getFinger(i);
        if (foundFinger instanceof InternalPointer) {
            // get existing finger in that slot... if it's not self, remove it... removing it should automatically shift the next finger in
            // to its place
            if (oldFinger instanceof ExternalPointer) {
                state.removeFinger((ExternalPointer) oldFinger);
            }
        } else if (foundFinger instanceof ExternalPointer) {
            state.putFinger((ExternalPointer) foundFinger);
        } else {
            throw new IllegalStateException();
        }
        
        LOG.debug("{} {} - Finger index {} updated\nExpected: {}\nBefore: {}\nAfter: {}", state.getSelfId(), sourceId, i, findId, oldFinger,
                foundFinger);
    }
    
    @Override
    public String getId() {
        return sourceId;
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        new SleepSubcoroutine.Builder()
                .id(sourceId)
                .duration(duration)
                .timerAddressPrefix(state.getTimerPrefix())
                .build()
                .run(cnt);
    }

    private Pointer funnelToRouteToSuccessorCoroutine(Continuation cnt, NodeId findId) throws Exception {
        String idSuffix = "routetosucc" + state.generateExternalMessageId();
        RouteToSuccessorTask innerCoroutine = new RouteToSuccessorTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                findId);
        return innerCoroutine.run(cnt);
    }
}
