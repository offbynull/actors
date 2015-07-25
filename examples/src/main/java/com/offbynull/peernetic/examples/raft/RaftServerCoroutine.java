package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.Mode.FOLLOWER;
import static com.offbynull.peernetic.examples.raft.Mode.LEADER;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.PullEntryResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.RedirectResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RetryResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.PushEntryResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteResponse;
import com.offbynull.peernetic.examples.raft.internalmessages.Kill;
import com.offbynull.peernetic.examples.raft.internalmessages.StartServer;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.set.UnmodifiableSet;

public final class RaftServerCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        StartServer start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerPrefix();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        UnmodifiableSet<String> nodeLinks = start.getNodeLinks();
        AddressTransformer addressTransformer = start.getAddressTransformer();
        long seed = start.getSeed();

        Address self = ctx.getSelf();
        String selfLink = addressTransformer.selfAddressToLinkId(self);

        State state = new State(timerAddress, graphAddress, logAddress, seed, selfLink, nodeLinks, addressTransformer);

        ctx.addOutgoingMessage(logAddress, debug("Starting server"));
        ctx.addOutgoingMessage(graphAddress, new AddNode(selfLink));
        ctx.addOutgoingMessage(graphAddress, new MoveNode(selfLink, 0.0, 0.0));
        
        // initialize to follower
        state.setMode(FOLLOWER);
        CoroutineRunner modeCoroutineRunner = createModeCoroutineRunner(ctx, selfLink, state);
        
        try {
            while (true) {
                cnt.suspend();

                Object msg = ctx.getIncomingMessage();
                Address src = ctx.getSource();
                Address dst = ctx.getDestination();
                boolean isFromSelf = ctx.getSource().equals(ctx.getSelf());

                ctx.addOutgoingMessage(logAddress, debug("({}) Processing {} from {} to {}", state.getMode(), msg.getClass(),
                        src, dst));

                // Execute mode-specific logic
                while (!modeCoroutineRunner.execute()) {
                    modeCoroutineRunner = createModeCoroutineRunner(ctx, selfLink, state);
                    ctx.addOutgoingMessage(logAddress, debug("({}) Transitioned to mode", state.getMode()));
                    // should call execute again once back to the top of this while loop, meaning that new subcoroutine is properly primed
                    // and switches between modes during priming are properly handled (although mode switching during prime should never happen)
                }

                // Handle messages
                if (msg instanceof AppendEntriesRequest) {
                    ctx.addOutgoingMessage(logAddress, debug("Received append entries from {}", src));

                    AppendEntriesRequest aeReq = (AppendEntriesRequest) msg;

                    // If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
                    int term = aeReq.getTerm();
                    if (state.updateCurrentTerm(term)) {
                        state.setMode(FOLLOWER);
                        modeCoroutineRunner = createModeCoroutineRunner(ctx, selfLink, state);
                        continue;
                    }

                    // 1. Reply false if  term < currentTerm
                    int currentTerm = state.getCurrentTerm();
                    if (term < currentTerm) {
                        ctx.addOutgoingMessage(logAddress,
                                debug("Failed append entries. Sent term: {} vs Current term: {}", term, currentTerm));
                        ctx.addOutgoingMessage(src, new AppendEntriesResponse(currentTerm, false));
                        continue;
                    }

                    // 2. Reply false if log doesn't contain an entry at prevLogIndex whose term matches prevLogTerm
                    int prevLogIndex = aeReq.getPrevLogIndex();
                    int prevLogTerm = aeReq.getPrevLogTerm();

                    if (!state.containsLogEntry(prevLogIndex)) {
                        ctx.addOutgoingMessage(logAddress,
                                debug("Failed append entries. No log entry at {}", prevLogIndex));
                        ctx.addOutgoingMessage(src, new AppendEntriesResponse(currentTerm, false));
                        continue;
                    }

                    LogEntry logEntry = state.getLogEntry(prevLogIndex);
                    int logEntryTerm = logEntry.getTerm();

                    if (logEntryTerm != prevLogTerm) {
                        ctx.addOutgoingMessage(logAddress,
                                debug("Failed append entries. Terms @ idx {} do not match: {} vs {}", prevLogIndex, prevLogTerm,
                                        logEntryTerm));
                        ctx.addOutgoingMessage(src, new AppendEntriesResponse(currentTerm, false));
                        continue;
                    }

                    // 3. If an existing entry conflicts with a new one (same index but different terms), delete the existing entry and all
                    //    that follow it
                    UnmodifiableList<LogEntry> newLogEntries = aeReq.getEntries();

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
                    int leaderCommit = aeReq.getLeaderCommit();
                    int commitIndex = state.getCommitIndex();

                    if (leaderCommit > commitIndex) {
                        int indexofLastNewEntry = prevLogIndex + newLogEntries.size();
                        commitIndex = Math.min(leaderCommit, indexofLastNewEntry);
                        state.setCommitIndex(commitIndex);
                    }

                    state.setMode(FOLLOWER);
                    modeCoroutineRunner = createModeCoroutineRunner(ctx, selfLink, state);
                } else if (msg instanceof RequestVoteRequest) {
                    ctx.addOutgoingMessage(logAddress, debug("Received request vote from {}", src));

                    RequestVoteRequest rvReq = (RequestVoteRequest) msg;

                    // If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
                    int term = rvReq.getTerm();
                    if (state.updateCurrentTerm(term)) {
                        state.setMode(FOLLOWER);
                        modeCoroutineRunner = createModeCoroutineRunner(ctx, selfLink, state);
                    }

                    // 1. Reply false if  term < currentTerm
                    int currentTerm = state.getCurrentTerm();
                    if (term < currentTerm) {
                        ctx.addOutgoingMessage(logAddress,
                                debug("Failed request vote. Sent term: {} vs Current term: {}", term, currentTerm));
                        ctx.addOutgoingMessage(src, new RequestVoteResponse(currentTerm, false));
                        continue;
                    }

                    // 2. If votedFor is (null or candidateId), and candidate's log is at least as up-to-date as receiver's log, grant vote
                    String votedForLinkId = state.getVotedForLinkId();
                    Address baseSrc = src.removeSuffix(5); // actor:0:router:candidate:012:mrsr:345 -> actor:0
                    String candidateId = state.getAddressTransformer().remoteAddressToLinkId(baseSrc);
                    boolean votedForCondition = votedForLinkId == null || votedForLinkId.equals(candidateId);
                    boolean upToDateLogCondition;
                    if (state.isLogEmpty()) {
                        upToDateLogCondition = true;
                    } else {
                        int selfLastLogIndex = state.getLastLogIndex();
                        int selfLastLogTerm = state.getLastLogEntry().getTerm();
                        int otherLastLogIndex = rvReq.getLastLogIndex();
                        int otherLastLogTerm = rvReq.getLastLogTerm();

                        // as specified in 5.4.1
                        if (otherLastLogTerm > selfLastLogTerm) {
                            upToDateLogCondition = true;
                        } else if (otherLastLogTerm == selfLastLogTerm && otherLastLogIndex > selfLastLogIndex) {
                            upToDateLogCondition = true;
                        } else {
                            upToDateLogCondition = false;
                        }
                    }

                    boolean voteGranted = false;
                    if (votedForCondition && upToDateLogCondition) {
                        state.setVotedForLinkId(candidateId);
                    }

                    ctx.addOutgoingMessage(src, new RequestVoteResponse(currentTerm, voteGranted));
                } else if (msg instanceof PushEntryRequest) {
                    switch (state.getMode()) {
                        case FOLLOWER:
                            String leaderLinkId = state.getVotedForLinkId();
                            if (leaderLinkId == null) {
                                ctx.addOutgoingMessage(dst, src, new RetryResponse());
                            } else {
                                ctx.addOutgoingMessage(dst, src, new RedirectResponse(leaderLinkId));
                            }
                            break;
                        case CANDIDATE:
                            ctx.addOutgoingMessage(dst, src, new RetryResponse());
                            break;
                        case LEADER:
                            int term = state.getCurrentTerm();
                            Object value = ((PushEntryRequest) msg).getValue();
                            state.addLogEntries(new LogEntry(term, value));
                            int index = state.getLastLogIndex();
                            ctx.addOutgoingMessage(dst, src, new PushEntryResponse(term, index));
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                } else if (msg instanceof PullEntryRequest) {
                    switch (state.getMode()) {
                        case FOLLOWER:
                            String leaderLinkId = state.getVotedForLinkId();
                            if (leaderLinkId == null) {
                                ctx.addOutgoingMessage(dst, src, new RetryResponse());
                            } else {
                                ctx.addOutgoingMessage(dst, src, new RedirectResponse(leaderLinkId));
                            }
                            break;
                        case CANDIDATE:
                            ctx.addOutgoingMessage(dst, src, new RetryResponse());
                            break;
                        case LEADER:
                            int index = state.getCommitIndex();
                            LogEntry logEntry = state.getLogEntry(index);
                            int term = logEntry.getTerm();
                            Object value = logEntry.getValue();
                            ctx.addOutgoingMessage(dst, src, new PullEntryResponse(value, index, term));
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                } else if (isFromSelf && msg instanceof Kill) {
                    throw new RuntimeException("Kill message encountered");
                }
            }
        } finally {
            ctx.addOutgoingMessage(graphAddress, new RemoveNode(selfLink));
        }
    }

    private CoroutineRunner createModeCoroutineRunner(Context ctx, String selfLink, State state) {
        Address graphAddress = state.getGraphAddress();
        Mode newMode = state.getMode();

        Subcoroutine<?> subcoroutine;
        switch (newMode) {
            case FOLLOWER:
                subcoroutine = new FollowerSubcoroutine(state);
                ctx.addOutgoingMessage(graphAddress, new StyleNode(selfLink, "-fx-background-color: blue"));
                break;
            case CANDIDATE:
                subcoroutine = new CandidateSubcoroutine(state);
                ctx.addOutgoingMessage(graphAddress, new StyleNode(selfLink, "-fx-background-color: yellow"));
                break;
            case LEADER:
                subcoroutine = new LeaderSubcoroutine(state);
                ctx.addOutgoingMessage(graphAddress, new StyleNode(selfLink, "-fx-background-color: green"));
                break;
            default:
                throw new IllegalStateException(); // should never happen
        }

        CoroutineRunner modeCoroutineRunner = new CoroutineRunner(innerCnt -> subcoroutine.run(innerCnt));
        modeCoroutineRunner.setContext(ctx);

        return modeCoroutineRunner;
    }
}
