package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import org.apache.commons.lang3.Validate;

final class IncomingRequestHandlerSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = ROUTER_HANDLER_RELATIVE_ADDRESS;

    private final Address logAddress;

    public IncomingRequestHandlerSubcoroutine(State state) {
        Validate.notNull(state);
        this.logAddress = state.getLogAddress();
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}
