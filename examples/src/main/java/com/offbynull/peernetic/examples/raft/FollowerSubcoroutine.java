package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_FOLLOWER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.raft.externalmessages.HeartbeatRequest;
import com.offbynull.peernetic.examples.raft.internalmessages.ElectionTimeout;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class FollowerSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = ROUTER_FOLLOWER_RELATIVE_ADDRESS;

    private final State state;
    
    private final Address timerAddress;
    private final Address logAddress;
    private final Random random;
    private final Controller controller;

    public FollowerSubcoroutine(State state) {
        Validate.notNull(state);
        
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.random = state.getRandom();
        this.controller = state.getRouterController();
    }
    
    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Entering follower state"));
        
        top:
        while (true) {
            ElectionTimeout timeoutObj = new ElectionTimeout();
            int waitTime = randBetween(150, 300);
            ctx.addOutgoingMessage(SUB_ADDRESS, timerAddress.appendSuffix("" + waitTime), timeoutObj);
            ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Waiting {}ms for communication from leader", waitTime));

            while (true) {
                cnt.suspend();
                Object incomingMsg = ctx.getIncomingMessage();
                if (incomingMsg == timeoutObj) {
                    // The timeout has been hit without a heartbeat coming in. Switch to candidate mode.
                    ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Failed to receive communication from leader", waitTime));
                    
                    controller.add(new CandidateSubcoroutine(state), ADD_PRIME_NO_FINISH);
                    return null;
                } else if (incomingMsg instanceof HeartbeatRequest) {
                    // A heartbeat message as come in, reset.
                    ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Received communication from leader", waitTime));
                    continue top;
                }
            }
        }
    }
    
    private int randBetween(int start, int end) {
        return random.nextInt(end - start) + start;
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}
