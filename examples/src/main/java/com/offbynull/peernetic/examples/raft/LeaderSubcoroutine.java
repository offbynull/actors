package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.ForwardResult;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class LeaderSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = ROUTER_HANDLER_RELATIVE_ADDRESS; // this subcoroutine ran as part of handler
    private static final Address MSG_ROUTER_ADDRESS = SUB_ADDRESS.appendSuffix("messager");

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
        
        sendInitialKeepAlive(cnt);
        
        while (true) {
            sendUpdates(cnt);
        }
    }
    
    private void sendInitialKeepAlive(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // send empty appendentries to keep-alive
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Sending initial keep-alive append entries"));
        SubcoroutineRouter msgRouter = new SubcoroutineRouter(MSG_ROUTER_ADDRESS, ctx);
        int attempts = 5;
        int waitTimePerAttempt = state.getMinimumElectionTimeout() / attempts; // minimum election timeout / number of attempts
        int term = state.getCurrentTerm();
        int commitIndex = state.getCommitIndex();
        for (String linkId : state.getOtherNodeLinkIds()) {
            ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Sending keep-alive append entries to {}", linkId));
            List<LogEntry> entries = Collections.emptyList();
            int prevLogIndex = state.getNextIndex(linkId) - 1;
            int prevLogTerm = prevLogIndex == -1 ? -1 : state.getLogEntry(prevLogIndex).getTerm();
            AppendEntriesRequest req = new AppendEntriesRequest(term, prevLogIndex, prevLogTerm, entries, commitIndex);
            RequestSubcoroutine<AppendEntriesResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<AppendEntriesResponse>()
                    .timerAddressPrefix(timerAddress)
                    .attemptInterval(Duration.ofMillis(waitTimePerAttempt))
                    .maxAttempts(5)
                    .request(req)
                    .addExpectedResponseType(AppendEntriesResponse.class)
                    .address(MSG_ROUTER_ADDRESS.appendSuffix(linkId))
                    .build();
            msgRouter.getController().add(requestSubcoroutine, ADD_PRIME_NO_FINISH);
        }
        
        // wait for responses or failure
        while (true) {
            msgRouter.forward();
            if (msgRouter.getController().size() == 0) {
                break;
            }
            cnt.suspend();
        }
    }
    
    private void sendUpdates(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // send empty appendentries to keep-alive
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Sending normal append entries"));
        SubcoroutineRouter msgRouter = new SubcoroutineRouter(MSG_ROUTER_ADDRESS, ctx);
        int attempts = 5;
        int waitTimePerAttempt = state.getMinimumElectionTimeout() / attempts; // minimum election timeout / number of attempts
        int term = state.getCurrentTerm();
        int commitIndex = state.getCommitIndex();
        int lastLogIndex = state.getLastLogIndex();
        for (String linkId : state.getOtherNodeLinkIds()) {
            ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Sending normal append entries to {}", linkId));
            int nextLogIndex = state.getNextIndex(linkId);
            int prevLogIndex = nextLogIndex - 1;
            int prevLogTerm = prevLogIndex == -1 ? -1 : state.getLogEntry(prevLogIndex).getTerm();
            List<LogEntry> entries = prevLogIndex == -1 ? Collections.emptyList() : state.getTailLogEntries(nextLogIndex);
            AppendEntriesRequest req = new AppendEntriesRequest(term, prevLogIndex, prevLogTerm, entries, commitIndex);
            RequestSubcoroutine<AppendEntriesResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<AppendEntriesResponse>()
                    .timerAddressPrefix(timerAddress)
                    .attemptInterval(Duration.ofMillis(waitTimePerAttempt))
                    .maxAttempts(5)
                    .request(req)
                    .addExpectedResponseType(AppendEntriesResponse.class)
                    .address(MSG_ROUTER_ADDRESS.appendSuffix(linkId))
                    .build();
            msgRouter.getController().add(requestSubcoroutine, ADD_PRIME_NO_FINISH);
        }
        
        // wait for responses or failure
        while (true) {
            ForwardResult fr = msgRouter.forward();
            if (fr.isForwarded() && fr.isCompleted()) {
                Address reqAddress = fr.getSubcoroutine().getAddress();
                String linkId = reqAddress.getElement(reqAddress.size() - 1); // linkid used for last element
                AppendEntriesResponse resp = (AppendEntriesResponse) fr.getResult();
                if (resp.isSuccess()) {
                    state.setMatchIndex(linkId, lastLogIndex);
                    state.setNextIndex(linkId, lastLogIndex + 1);
                } else {
                    state.decrementNextIndex(linkId);
                }
            }
            if (msgRouter.getController().size() == 0) {
                break;
            }
            cnt.suspend();
        }
        
        
        TODO UPDATE COMMIT INDEX HERE
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}
