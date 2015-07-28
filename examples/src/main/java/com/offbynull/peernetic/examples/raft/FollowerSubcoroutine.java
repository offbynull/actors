package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.Mode.CANDIDATE;
import com.offbynull.peernetic.examples.raft.internalmessages.ElectionTimeout;
import org.apache.commons.lang3.Validate;

final class FollowerSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = Address.of(); // empty

    private final ServerState state;
    
    private final Address timerAddress;
    private final Address logAddress;

    public FollowerSubcoroutine(ServerState state) {
        Validate.notNull(state);
        
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
    }
    
    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Entering follower mode"));
        
        top:
        while (true) {
            // When entering follower state (or re-entering if we got a higher term from a msg), clear votedfor so new vote requests can
            // get processed properly.
            state.setVotedForLinkId(null);
            
            // Set up timeout
            ElectionTimeout timeoutObj = new ElectionTimeout();
            int waitTime = state.nextElectionTimeout();
            ctx.addOutgoingMessage(SUB_ADDRESS, timerAddress.appendSuffix("" + waitTime), timeoutObj);

            while (true) {
                cnt.suspend();

                Object msg = ctx.getIncomingMessage();
                if (msg == timeoutObj) {
                    // The timeout has been hit without a heartbeat coming in. Switch to candidate mode and exit.
                    ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Election timeout elapsed, switching to candidate", waitTime));
                    
                    state.setMode(CANDIDATE);
                    return null;
                }
            }
        }
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}
