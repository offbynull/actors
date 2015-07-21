package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_FOLLOWER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.AppendEntriesResponse;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteResponse;
import com.offbynull.peernetic.examples.raft.internalmessages.ElectionTimeout;
import java.util.List;
import java.util.Random;
import org.apache.commons.collections4.list.UnmodifiableList;
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
            // When entering follower state (or re-entering if we got a higher term from a msg), clear votedfor so new vote requests can
            // get processed properly.
            state.setVotedForLinkId(null);
            
            // Set up timeout
            ElectionTimeout timeoutObj = new ElectionTimeout();
            int waitTime = randBetween(150, 300);
            ctx.addOutgoingMessage(SUB_ADDRESS, timerAddress.appendSuffix("" + waitTime), timeoutObj);
            ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Waiting {}ms for communication from leader", waitTime));

            while (true) {
                cnt.suspend();
                Address src = ctx.getSource();
                Object incomingMsg = ctx.getIncomingMessage();
                if (incomingMsg == timeoutObj) {
                    // The timeout has been hit without a heartbeat coming in. Switch to candidate mode.
                    ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Failed to receive communication from leader", waitTime));
                    
                    controller.add(new CandidateSubcoroutine(state), ADD_PRIME_NO_FINISH);
                    return null;
                } else if (incomingMsg instanceof AppendEntriesRequest) {
                    ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Received append entries from {}", src));
                    
                    AppendEntriesRequest aeReq = (AppendEntriesRequest) incomingMsg;
                    
                    // If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
                    int term = aeReq.getTerm();
                    if (state.updateCurrentTerm(term)) {
                        continue top;
                    }
                    
                    // 1. Reply false if  term < currentTerm
                    int currentTerm = state.getCurrentTerm();
                    if (term < currentTerm) {
                        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress,
                                debug("Failed append entries. Sent term: {} vs Current term: {}", term, currentTerm));
                        ctx.addOutgoingMessage(SUB_ADDRESS, src, new AppendEntriesResponse(currentTerm, false));
                        continue;
                    }
                    
                    // 2. Reply false if log doesn't contain an entry at prevLogIndex whose term matches prevLogTerm
                    int prevLogIndex = aeReq.getPrevLogIndex();
                    int prevLogTerm = aeReq.getPrevLogTerm();
                    
                    if (!state.containsLogEntry(prevLogIndex)) {
                        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress,
                                debug("Failed append entries. No log entry at {}", prevLogIndex));
                        ctx.addOutgoingMessage(SUB_ADDRESS, src, new AppendEntriesResponse(currentTerm, false));
                        continue;                        
                    }

                    LogEntry logEntry = state.getLogEntry(prevLogIndex);
                    int logEntryTerm = logEntry.getTerm();
                    
                    if (logEntryTerm != prevLogTerm) {
                        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress,
                                debug("Failed append entries. Terms @ idx {} do not match: {} vs {}", prevLogIndex, prevLogTerm,
                                        logEntryTerm));
                        ctx.addOutgoingMessage(SUB_ADDRESS, src, new AppendEntriesResponse(currentTerm, false));
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
                            ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Truncating log from idx {} forward", logEntryIdx));
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
                    
                    
                    continue top;
                } else if (incomingMsg instanceof RequestVoteRequest) {
                    ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Received request vote from {}", src));
                    
                    RequestVoteRequest rvReq = (RequestVoteRequest) incomingMsg;

                    // If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
                    int term = rvReq.getTerm();
                    if (state.updateCurrentTerm(term)) {
                        continue top;
                    }
                    
                    // 1. Reply false if  term < currentTerm
                    int currentTerm = state.getCurrentTerm();
                    if (term < currentTerm) {
                        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress,
                                debug("Failed request vote. Sent term: {} vs Current term: {}", term, currentTerm));
                        ctx.addOutgoingMessage(SUB_ADDRESS, src, new RequestVoteResponse(currentTerm, false));
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
                        int selfLastLogIndex = state.getLogSize() - 1;
                        int selfLastLogTerm = state.getLogEntry(selfLastLogIndex).getTerm();
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
                    
                    ctx.addOutgoingMessage(SUB_ADDRESS, src, new RequestVoteResponse(currentTerm, voteGranted));
                    
                    
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
