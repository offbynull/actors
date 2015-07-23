package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesResponse;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class LeaderSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = ROUTER_HANDLER_RELATIVE_ADDRESS; // this subcoroutine ran as part of handler

    private final State state;
    
    private final Address timerAddress;
    private final Address logAddress;

    public LeaderSubcoroutine(State state) {
        Validate.notNull(state);
        
        this.state = state;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
    }
    
    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Entering leader mode"));
        
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Sending initial empty append entries"));
        Address msgRouterAddress = SUB_ADDRESS.appendSuffix("messager");
        SubcoroutineRouter msgRouter = new SubcoroutineRouter(msgRouterAddress, ctx);
        int totalWaitTime = state.randBetween(150, 300); // election timeout
        int attempts = 5;
        int waitTimePerAttempt = totalWaitTime / attempts; // divide by n attempts
        for (String linkId : state.getOtherNodeLinkIds()) {
            AppendEntriesRequest req = new AppendEntriesRequest(term, prevLogIndex, prevLogTerm, entries, leaderCommit);
            RequestSubcoroutine<AppendEntriesResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<AppendEntriesResponse>()
                    .timerAddressPrefix(timerAddress)
                    .attemptInterval(Duration.ofMillis(waitTimePerAttempt))
                    .maxAttempts(5)
                    .request(req)
                    .addExpectedResponseType(AppendEntriesResponse.class)
                    .address(msgRouterAddress.appendSuffix(linkId))
                    .build();
            msgRouter.getController().add(requestSubcoroutine,ADD_PRIME_NO_FINISH);
        }
        
        
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}
