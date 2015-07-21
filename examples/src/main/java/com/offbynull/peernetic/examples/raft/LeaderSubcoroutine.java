package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_LEADER_RELATIVE_ADDRESS;
import org.apache.commons.lang3.Validate;

final class LeaderSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = ROUTER_LEADER_RELATIVE_ADDRESS;

    private final State state;
    
    private final Address timerAddress;
    private final Address logAddress;
    private final Controller controller;
    
    public LeaderSubcoroutine(State state) {
        Validate.notNull(state);

        this.state = state;
        
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.controller = state.getRouterController();
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Entering leader state"));
        
        top:
        while (true) {
            // FILL THIS IN
        }
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}
