package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.ForwardResult;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryResponse;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LeaderSubcoroutine extends AbstractRaftServerSubcoroutine {

    private static final Address MSG_ROUTER_ADDRESS = Address.of("messager");

    public LeaderSubcoroutine(ServerState state) {
        super(state);
    }
    
    @Override
    protected Mode main(Continuation cnt, ServerState state) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Address logAddress = state.getLogAddress();
        Address graphAddress = state.getGraphAddress();
        String selfLink = state.getSelfLinkId();
        
        ctx.addOutgoingMessage(logAddress, debug("Entering leader mode"));
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfLink, "-fx-background-color: green"));
        
        if (state.getOtherNodeLinkIds().isEmpty()) {
            // single node in this cluster, there's nothing to send keepalives/updates to -- just endlessly sit here without doing anything
            ctx.addOutgoingMessage(logAddress, debug("No other nodes to be leader of"));
            while (true) {
                // update commit index right away, no other nodes to sync up to
                int lastIdx = state.getLastLogIndex();
                state.setCommitIndex(lastIdx);
                
                cnt.suspend();
            }
        } else {
            sendInitialKeepAlive(cnt, state);

            while (true) {
                sendUpdates(cnt, state);
            }
        }
    }

    private void sendInitialKeepAlive(Continuation cnt, ServerState state) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Address logAddress = state.getLogAddress();
        Address timerAddress = state.getTimerAddress();
        
        // send empty appendentries to keep-alive
        ctx.addOutgoingMessage(logAddress, debug("Sending initial keep-alive append entries"));
        SubcoroutineRouter msgRouter = new SubcoroutineRouter(MSG_ROUTER_ADDRESS, ctx);
        int attempts = 5;
        int waitTimePerAttempt = state.getMinimumElectionTimeout() / attempts; // minimum election timeout / number of attempts
        int term = state.getCurrentTerm();
        int commitIndex = state.getCommitIndex();
        IdGenerator idGenerator = state.getIdGenerator();
        for (String linkId : state.getOtherNodeLinkIds()) {
            ctx.addOutgoingMessage(logAddress, debug("Sending keep-alive append entries to {}", linkId));
            List<LogEntry> entries = Collections.emptyList();
            int prevLogIndex = state.getNextIndex(linkId) - 1;
            int prevLogTerm = state.getLogEntry(prevLogIndex).getTerm();
            Address dstAddress = state.getAddressTransformer().linkIdToRemoteAddress(linkId);
            AppendEntriesRequest req = new AppendEntriesRequest(term, prevLogIndex, prevLogTerm, entries, commitIndex);
            RequestSubcoroutine<AppendEntriesResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<AppendEntriesResponse>()
                    .timerAddress(timerAddress)
                    .attemptInterval(Duration.ofMillis(waitTimePerAttempt))
                    .maxAttempts(5)
                    .request(req)
                    .addExpectedResponseType(AppendEntriesResponse.class)
                    .sourceAddress(MSG_ROUTER_ADDRESS, idGenerator)
                    .destinationAddress(dstAddress)
                    .throwExceptionIfNoResponse(false)
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
    
    private void sendUpdates(Continuation cnt, ServerState state) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Address logAddress = state.getLogAddress();
        Address timerAddress = state.getTimerAddress();
        
        Map<Subcoroutine<?>, String> linkIdLookup = new HashMap<>(); // key = requstsubcoroutine, value = linkId
        
        // send updates
        SubcoroutineRouter msgRouter = new SubcoroutineRouter(MSG_ROUTER_ADDRESS, ctx);
        int attempts = 5;
        int waitTimePerAttempt = state.getMinimumElectionTimeout() / attempts; // minimum election timeout / number of attempts
        int term = state.getCurrentTerm();
        int commitIndex = state.getCommitIndex();
        int lastLogIndex = state.getLastLogIndex();
        IdGenerator idGenerator = state.getIdGenerator();
        for (String linkId : state.getOtherNodeLinkIds()) {
            ctx.addOutgoingMessage(logAddress, debug("Sending normal append entries to {}", linkId));
            int nextLogIndex = state.getNextIndex(linkId);
            int prevLogIndex = nextLogIndex - 1;
            int prevLogTerm = state.getLogEntry(prevLogIndex).getTerm();
            List<LogEntry> entries = nextLogIndex > lastLogIndex ? Collections.emptyList() : state.getTailLogEntries(nextLogIndex);
            Address dstAddress = state.getAddressTransformer().linkIdToRemoteAddress(linkId);
            AppendEntriesRequest req = new AppendEntriesRequest(term, prevLogIndex, prevLogTerm, entries, commitIndex);
            RequestSubcoroutine<AppendEntriesResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<AppendEntriesResponse>()
                    .timerAddress(timerAddress)
                    .attemptInterval(Duration.ofMillis(waitTimePerAttempt))
                    .maxAttempts(5)
                    .request(req)
                    .addExpectedResponseType(AppendEntriesResponse.class)
                    .sourceAddress(MSG_ROUTER_ADDRESS, idGenerator)
                    .destinationAddress(dstAddress)
                    .throwExceptionIfNoResponse(false)
                    .build();
            msgRouter.getController().add(requestSubcoroutine, ADD_PRIME_NO_FINISH);
            linkIdLookup.put(requestSubcoroutine, linkId);
        }
        
        // wait for responses or failure
        while (true) {
            ForwardResult fr = msgRouter.forward();
            if (fr.isForwarded() && fr.isCompleted()) {
                Subcoroutine<?> completedSubcoroutine = fr.getSubcoroutine();
                String linkId = linkIdLookup.get(completedSubcoroutine);
                AppendEntriesResponse resp = (AppendEntriesResponse) fr.getResult();
                if (resp != null) {
                    if (resp.isSuccess()) {
                        state.setMatchIndex(linkId, lastLogIndex);
                        state.setNextIndex(linkId, lastLogIndex + 1);
                    } else {
                        state.decrementNextIndex(linkId);
                    }
                }
            }
            if (msgRouter.getController().size() == 0) {
                break;
            }
            cnt.suspend();
        }
        
        // update commit index if majority responded with a certain commit index
        int minimumRequiredCount = state.getMajorityCount();
        for (int n = state.getCommitIndex() + 1; n <= state.getLastLogIndex(); n++) {
            int count = 1; // 1 for self
            
            for (String linkId : state.getOtherNodeLinkIds()) {
                if (state.getMatchIndex(linkId) >= n) {
                    count++;
                }
            }
            
            if (count >= minimumRequiredCount) {
                LogEntry logEntry = state.getLogEntry(n);
                if (logEntry.getTerm() == term) {
                    state.setCommitIndex(n);
                    break;
                }
            }
        }
    }

    @Override
    protected Mode handlePushEntryRequest(Context ctx, PushEntryRequest req, ServerState state) throws Exception {
        Address src = ctx.getSource();
        Address logAddress = state.getLogAddress();
        
        int term = state.getCurrentTerm();
        Object value = req.getValue();
        state.addLogEntries(new LogEntry(term, value));
        int index = state.getLastLogIndex();
        ctx.addOutgoingMessage(logAddress, debug("Responding with success {}/{}", term, index));
        ctx.addOutgoingMessage(src, new PushEntryResponse(index, term));
        
        return null;
    }

    @Override
    protected Mode handlePullEntryRequest(Context ctx, PullEntryRequest req, ServerState state) throws Exception {
        Address src = ctx.getSource();
        Address logAddress = state.getLogAddress();
        
        int index = state.getCommitIndex();
        LogEntry logEntry = state.getLogEntry(index);
        int term = logEntry.getTerm();
        Object value = logEntry.getValue();
        ctx.addOutgoingMessage(logAddress, debug("Responding with success {}/{}", term, index));
        ctx.addOutgoingMessage(src, new PullEntryResponse(value, index, term));
        
        return null;
    }
}
