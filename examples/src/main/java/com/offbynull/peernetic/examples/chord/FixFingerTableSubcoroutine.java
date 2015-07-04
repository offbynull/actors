package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.error;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class FixFingerTableSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;

    public FixFingerTableSubcoroutine(Address sourceId, State state, Address timerAddress, Address logAddress) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        this.sourceId = sourceId;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Starting fix finger task", state.getSelfId(), sourceId));
        
        int len = state.getFingerTableLength();

        while (true) {
            List<Pointer> oldFingers = state.getFingers();
            for (int i = 1; i < len; i++) {
                ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Fixing finger {}", state.getSelfId(), sourceId, i));
                fixFinger(cnt, i);
            }

            ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Fix finger cycle \\ Before: {} \\ After: {}", state.getSelfId(),
                    sourceId, oldFingers, state.getFingers()));
            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
        }
    }
    
    private void fixFinger(Continuation cnt, int i) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // get expected id of entry in finger table
        NodeId findId = state.getExpectedFingerId(i);

        // route to id
        Pointer foundFinger;
        try {
            foundFinger = funnelToRouteToSuccessorCoroutine(cnt, findId);
        } catch (RuntimeException re) {
            ctx.addOutgoingMessage(sourceId, logAddress, error("{} {} - Error while finding index for {}", state.getSelfId(), sourceId, i));
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
        
        ctx.addOutgoingMessage(sourceId, logAddress, debug("{} {} - Finger index {} updated \\ Expected: {} \\ Before: {} \\ After: {}",
                state.getSelfId(), sourceId, i, findId, oldFinger, foundFinger));
    }
    
    @Override
    public Address getId() {
        return sourceId;
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        new SleepSubcoroutine.Builder()
                .id(sourceId)
                .duration(duration)
                .timerAddressPrefix(timerAddress)
                .build()
                .run(cnt);
    }

    private Pointer funnelToRouteToSuccessorCoroutine(Continuation cnt, NodeId findId) throws Exception {
        String idSuffix = "routetosucc" + state.nextRandomId();
        RouteToSuccessorSubcoroutine innerCoroutine = new RouteToSuccessorSubcoroutine(
                sourceId.appendSuffix(idSuffix),
                state,
                timerAddress,
                logAddress,
                findId);
        return innerCoroutine.run(cnt);
    }
}
