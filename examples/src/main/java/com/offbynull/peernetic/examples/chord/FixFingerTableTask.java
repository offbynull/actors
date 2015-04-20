package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.coroutines.SleepCoroutine;
import java.time.Duration;
import java.util.List;
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
            List<Pointer> oldFingers = state.getFingers();
            for (int i = 1; i < len; i++) {
//                // Fix successor (finger idx 0) and the finger at i. If i == 0, this will fix the successor twice, which is not really
//                // required but it doesn't hurt.
//                
                LOG.debug("{} {} - Fixing successor", state.getSelfId(), sourceId);
                fixSuccessor(cnt);
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

    private void fixSuccessor(Continuation cnt) throws Exception {
        // get expected id of entry in finger table
        NodeId findId = state.getExpectedFingerId(0);

        // route to id
        Pointer foundFinger;
        try {
            // Use init route to successor because it doesn't take self in to account when doing route. Assume that our successor is an
            // external pointer. If it isn't (if our successor is ourself) a classcast exception will be thrown and caught and subsequent
            // processing should be correct
            foundFinger = funnelToSkipRouteToSuccessorCoroutine(cnt, (ExternalPointer) state.getSuccessor(), findId);
        } catch (RuntimeException re) {
            LOG.error("{} {} - Error routing to successor, removing successor", state.getSelfId(), sourceId);
            foundFinger = null;
        }
        
        Pointer oldSuccessor = state.getSuccessor();
        if (foundFinger == null) {
            // Routing failed. Since we're dealing with our successor, simply shift to the next successor in the successor table.
            state.moveToNextSuccessor();
        } else {
            if (foundFinger instanceof InternalPointer) {
                state.moveToNextSuccessor();
            } else if (foundFinger instanceof ExternalPointer) {
                state.putFinger((ExternalPointer) foundFinger);
            } else {
                throw new IllegalStateException();
            }
        }
        
        LOG.debug("{} {} - Finger index 0 (successor) updated\nExpected: {}\nBefore: {}\nAfter: {}", state.getSelfId(), sourceId, findId,
                oldSuccessor, foundFinger);
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        
        SleepCoroutine sleepCoroutine = new SleepCoroutine(sourceId, state.getTimerPrefix(), duration);
        sleepCoroutine.run(cnt);
    }

    private Pointer funnelToRouteToSuccessorCoroutine(Continuation cnt, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "routetosucc" + state.generateExternalMessageId();
        RouteToSuccessorTask innerCoroutine = new RouteToSuccessorTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                findId);
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
    
    private Pointer funnelToSkipRouteToSuccessorCoroutine(Continuation cnt, ExternalPointer bootstrapNode, NodeId findId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(findId);
        
        String idSuffix = "skiproutetosucc" + state.generateExternalMessageId();
        SkipRouteToSuccessorTask innerCoroutine = new SkipRouteToSuccessorTask(
                AddressUtils.parentize(sourceId, idSuffix),
                state,
                bootstrapNode,
                findId,
                state.getSelfId());
        innerCoroutine.run(cnt);
        return innerCoroutine.getResult();
    }
}
