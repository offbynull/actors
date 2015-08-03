package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineStepper;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.Mode.FOLLOWER;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteResponse;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

abstract class AbstractRaftServerSubcoroutine implements Subcoroutine<Mode> {

    private static final Address EMPTY_ADDRESS = Address.of(); // empty

    private final ServerState state;

    public AbstractRaftServerSubcoroutine(ServerState state) {
        Validate.notNull(state);

        this.state = state;
    }

    @Override
    public final Mode run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Address logAddress = state.getLogAddress();

        Subcoroutine<Mode> mainSubcoroutine = new Subcoroutine<Mode>() {

            @Override
            public Address getAddress() {
                return EMPTY_ADDRESS;
            }

            @Override
            public Mode run(Continuation cnt) throws Exception {
                return main(cnt, state);
            }
        };
        SubcoroutineStepper<Mode> mainStepper = new SubcoroutineStepper<>(ctx, mainSubcoroutine);

        while (true) {
            Address src = ctx.getSource();
            Object msg = ctx.getIncomingMessage();

            Mode ret = null;
            if (msg instanceof AppendEntriesRequest) {
                ctx.addOutgoingMessage(logAddress, debug("Received append entries from {}", src));
                ret = handleAppendEntriesRequest(ctx, (AppendEntriesRequest) msg, state);
            } else if (msg instanceof RequestVoteRequest) {
                ctx.addOutgoingMessage(logAddress, debug("Received request vote from {}", src));
                ret = handleRequestVoteRequest(ctx, (RequestVoteRequest) msg, state);
            } else if (msg instanceof PushEntryRequest) {
                ctx.addOutgoingMessage(logAddress, debug("Received push entry from {}", src));
                ret = handlePushEntryRequest(ctx, (PushEntryRequest) msg, state);
            } else if (msg instanceof PullEntryRequest) {
                ctx.addOutgoingMessage(logAddress, debug("Received pull entry from {}", src));
                ret = handlePullEntryRequest(ctx, (PullEntryRequest) msg, state);
            } else {
                if (!mainStepper.step()) {
                    ret = mainStepper.getResult();
                }
            }

            if (ret != null) {
                return ret;
            }
            
            cnt.suspend();
        }
    }

    @Override
    public final Address getAddress() {
        return EMPTY_ADDRESS;
    }

    protected abstract Mode main(Continuation cnt, ServerState state) throws Exception;

    protected Mode handleAppendEntriesRequest(Context ctx, AppendEntriesRequest req, ServerState state) throws Exception {
        String selfLink = state.getSelfLinkId();
        Address src = ctx.getSource();
        Address graphAddress = state.getGraphAddress();
        Address logAddress = state.getLogAddress();

        // If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
        int term = req.getTerm();
        state.updateCurrentTerm(term); // will always switch to follower if it makes it to the end

        // 1. Reply false if  term < currentTerm
        int currentTerm = state.getCurrentTerm();
        if (term < currentTerm) {
            ctx.addOutgoingMessage(logAddress,
                    debug("Failed append entries. Sent term: {} vs Current term: {}", term, currentTerm));
            ctx.addOutgoingMessage(src, new AppendEntriesResponse(currentTerm, false));
            return null;
        }

        // 2. Reply false if log doesn't contain an entry at prevLogIndex whose term matches prevLogTerm
        int prevLogIndex = req.getPrevLogIndex();
        int prevLogTerm = req.getPrevLogTerm();

        if (!state.containsLogEntry(prevLogIndex)) {
            ctx.addOutgoingMessage(logAddress,
                    debug("Failed append entries. No log entry at {}", prevLogIndex));
            ctx.addOutgoingMessage(src, new AppendEntriesResponse(currentTerm, false));
            return null;
        }

        LogEntry logEntry = state.getLogEntry(prevLogIndex);
        int logEntryTerm = logEntry.getTerm();

        if (logEntryTerm != prevLogTerm) {
            ctx.addOutgoingMessage(logAddress,
                    debug("Failed append entries. Terms @ idx {} do not match: {} vs {}", prevLogIndex, prevLogTerm,
                            logEntryTerm));
            ctx.addOutgoingMessage(src, new AppendEntriesResponse(currentTerm, false));
            return null;
        }

        // 3. If an existing entry conflicts with a new one (same index but different terms), delete the existing entry and all
        //    that follow it
        UnmodifiableList<LogEntry> newLogEntries = req.getEntries();

        // find first conflicting entry
        int firstNonEncounteredNewEntryIdx = 0;
        for (LogEntry newLogEntry : newLogEntries) {
            int logEntryIdx = prevLogIndex + firstNonEncounteredNewEntryIdx + 1;
            if (!state.containsLogEntry(logEntryIdx)) {
                // no more entries
                break;
            }

            LogEntry existingLogEntry = state.getLogEntry(logEntryIdx);
            int existingLogEntryTerm = existingLogEntry.getTerm();
            int newLogEntryTerm = newLogEntry.getTerm();

            if (existingLogEntryTerm != newLogEntryTerm) {
                // found conflicting entry, clear everything from this index forward
                ctx.addOutgoingMessage(logAddress, debug("Truncating log from idx {} forward", logEntryIdx));
                state.truncateLogEntries(logEntryIdx);
                break;
            }

            firstNonEncounteredNewEntryIdx++;
        }

        // 4. Append any new entries not already in the log.
        List<LogEntry> newLogEntriesToAdd = newLogEntries.subList(firstNonEncounteredNewEntryIdx, newLogEntries.size());
        state.addLogEntries(newLogEntriesToAdd);

        // 5. If leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry)
        int leaderCommit = req.getLeaderCommit();
        int commitIndex = state.getCommitIndex();

        if (leaderCommit > commitIndex) {
            int indexofLastNewEntry = prevLogIndex + newLogEntries.size();
            commitIndex = Math.min(leaderCommit, indexofLastNewEntry);
            state.setCommitIndex(commitIndex);
        }

        ctx.addOutgoingMessage(logAddress, debug("Responding with Term: {} / Success: {}", currentTerm, true));
        ctx.addOutgoingMessage(src, new AppendEntriesResponse(currentTerm, true));

        // No matter what state youre in, if you get a new appendentries (>= to that ofyour current term), switch your mode to
        // follower mode (resets election timeout if already in follower mode) and assume that this is the node in
        // the server that's the leader. Otherwise, why would it be sending you appendentries? RAFT paper @ 5.2 paragraph 4
        Address baseSrc = src.removeSuffix(2); // actor:1:messager:-149223987249938403 -> actor:1
        String newVoterId = state.getAddressTransformer().remoteAddressToLinkId(baseSrc);
        String oldVotedForId = state.getVotedForLinkId();

        if (!newVoterId.equals(oldVotedForId)) {
            if (oldVotedForId != null) {
                ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, oldVotedForId));
            }
            ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLink, newVoterId));
        }

        state.setVotedForLinkId(newVoterId);
        
        return FOLLOWER;
    }

    protected Mode handleRequestVoteRequest(Context ctx, RequestVoteRequest req, ServerState state) throws Exception {
        String selfLink = state.getSelfLinkId();
        Address src = ctx.getSource();
        Address graphAddress = state.getGraphAddress();
        Address logAddress = state.getLogAddress();

        // If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
        int term = req.getTerm();
        boolean switchToFollower = false;
        if (state.updateCurrentTerm(term)) {
            // No matter what state youre in, if you get a new vote request (higher term that your current term), switch your
            // mode to follower mode (resets election timeout if already in follower mode) and unset whoever is your current
            // leader.
            switchToFollower = true;

            String oldVotedForId = state.getVotedForLinkId();
            if (oldVotedForId != null) {
                ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, oldVotedForId));
            }
            state.setVotedForLinkId(null);
        }

        // 1. Reply false if  term < currentTerm
        int currentTerm = state.getCurrentTerm();
        if (term < currentTerm) {
            ctx.addOutgoingMessage(logAddress,
                    debug("Failed request vote. Sent term: {} vs Current term: {}", term, currentTerm));
            ctx.addOutgoingMessage(src, new RequestVoteResponse(currentTerm, false));
            return null;
        }

        // 2. If votedFor is (null or candidateId), and candidate's log is at least as up-to-date as receiver's log, grant vote
        String votedForLinkId = state.getVotedForLinkId();
        Address baseSrc = src.removeSuffix(3); // actor:0:4437113782736519168:mrsr:-7261648962812116991 -> actor:0
        String candidateId = state.getAddressTransformer().remoteAddressToLinkId(baseSrc);
        boolean votedForCondition = votedForLinkId == null || votedForLinkId.equals(candidateId);

        // as specified in 5.4.1
        int selfLastLogIndex = state.getLastLogIndex();
        int selfLastLogTerm = state.getLastLogEntry().getTerm();
        int otherLastLogIndex = req.getLastLogIndex();
        int otherLastLogTerm = req.getLastLogTerm();

        boolean candidateLogUpToDateOrBetter = false;
        if (otherLastLogTerm > selfLastLogTerm) {
            candidateLogUpToDateOrBetter = true;
        } else if (otherLastLogTerm == selfLastLogTerm && otherLastLogIndex >= selfLastLogIndex) {
            candidateLogUpToDateOrBetter = true;
        }

        boolean voteGranted = false;
        if (votedForCondition && candidateLogUpToDateOrBetter) {
            if (!candidateId.equals(votedForLinkId)) {
                if (votedForLinkId != null) {
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLink, votedForLinkId));
                }
                ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLink, candidateId));
            }

            state.setVotedForLinkId(candidateId);

            voteGranted = true;
        }

        ctx.addOutgoingMessage(logAddress, debug("Responding with Term: {} / Granted : {}", currentTerm, voteGranted));
        ctx.addOutgoingMessage(src, new RequestVoteResponse(currentTerm, voteGranted));

        return switchToFollower ? FOLLOWER : null;
    }

    protected abstract Mode handlePushEntryRequest(Context ctx, PushEntryRequest req, ServerState state) throws Exception;

    protected abstract Mode handlePullEntryRequest(Context ctx, PullEntryRequest req, ServerState state) throws Exception;
}
