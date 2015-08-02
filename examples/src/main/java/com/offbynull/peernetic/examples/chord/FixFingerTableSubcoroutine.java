package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.error;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class FixFingerTableSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    private final IdGenerator idGenerator;

    public FixFingerTableSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.idGenerator = state.getIdGenerator();
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Starting fix finger task", state.getSelfId(), subAddress));
        
        int len = state.getFingerTableLength();

        while (true) {
            List<Pointer> oldFingers = state.getFingers();
            for (int i = 1; i < len; i++) {
                ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Fixing finger {}", state.getSelfId(), subAddress, i));
                fixFinger(cnt, i);
            }

            ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Fix finger cycle \\ Before: {} \\ After: {}", state.getSelfId(),
                    subAddress, oldFingers, state.getFingers()));
            funnelToSleepCoroutine(cnt, Duration.ofSeconds(1L));
        }
    }
    
    private void fixFinger(Continuation cnt, int i) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // get expected address of entry in finger table
        NodeId findId = state.getExpectedFingerId(i);

        // route to address
        Pointer foundFinger;
        try {
            foundFinger = funnelToRouteToSuccessorCoroutine(cnt, findId);
        } catch (RuntimeException re) {
            ctx.addOutgoingMessage(subAddress, logAddress, error("{} {} - Error while finding index for {}", state.getSelfId(), subAddress, i));
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
        
        ctx.addOutgoingMessage(subAddress, logAddress, debug("{} {} - Finger index {} updated \\ Expected: {} \\ Before: {} \\ After: {}",
                state.getSelfId(), subAddress, i, findId, oldFinger, foundFinger));
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    private void funnelToSleepCoroutine(Continuation cnt, Duration duration) throws Exception {
        new SleepSubcoroutine.Builder()
                .address(subAddress)
                .duration(duration)
                .timerAddress(timerAddress)
                .build()
                .run(cnt);
    }

    private Pointer funnelToRouteToSuccessorCoroutine(Continuation cnt, NodeId findId) throws Exception {
        String idSuffix = "routetosucc" + idGenerator.generate();
        RouteToSuccessorSubcoroutine innerCoroutine = new RouteToSuccessorSubcoroutine(
                subAddress.appendSuffix(idSuffix),
                state,
                findId);
        return innerCoroutine.run(cnt);
    }
}
