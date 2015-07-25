package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import static com.offbynull.peernetic.examples.raft.Mode.FOLLOWER;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

final class State {
    
    private static final int MIN_ELECTION_TIMEOUT = 150;
    private static final int MAX_ELECTION_TIMEOUT = 300;
    
    private final Random random;
    private int counter = 0;
    
    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    private final String selfLinkId;
    private final UnmodifiableSet<String> otherNodeLinkIds; // does not include self
    private final AddressTransformer addressTransformer;
    
    private Mode mode;
    
    // persistent state (not really persistent in this example, but whatever)
    private int currentTerm;
    private String votedForLinkId;
    private LinkedList<LogEntry> log;
    
    // volatile state on all servers
    private int commitIndex;
    private int lastApplied;
    
    // volatile state on leaders (reinitialized after election)
    private Map<String, Integer> nextIndex; // key = node link id, value = idx of next log entry to send to that node
                                            // (init to leader last log index + 1)
    private Map<String, Integer> matchIndex; // key = node link id, value = idx of highest log entry known to be replicated on server
                                            // (init to 0, increases monotonically) 

    public State(Address timerAddress, Address graphAddress, Address logAddress, long seed, String selfLinkId,
            UnmodifiableSet<String> nodeLinkIds, AddressTransformer addressTransformer) {
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        Validate.notNull(selfLinkId);
        Validate.notNull(nodeLinkIds);
        Validate.notNull(addressTransformer);
        Validate.isTrue(nodeLinkIds.contains(selfLinkId));
        Validate.isTrue(nodeLinkIds.size() >= 2);
        
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
        this.random = new Random(seed);
        this.selfLinkId = selfLinkId;
        Set<String> modifiedNodeLinks = new HashSet<>(nodeLinkIds);
        modifiedNodeLinks.remove(selfLinkId);
        this.otherNodeLinkIds = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new HashSet<String>(modifiedNodeLinks));
        this.addressTransformer = addressTransformer;

        mode = FOLLOWER;
        
        currentTerm = 0;
        votedForLinkId = null; // can be null
        log = new LinkedList<>();
        log.add(new LogEntry(0, "uninitialized"));
        
        commitIndex = 0;
        lastApplied = 0;

        nextIndex = new HashMap<>();
        for (String linkId : otherNodeLinkIds) {
            nextIndex.put(linkId, 1);
        }
        
        matchIndex = new HashMap<>();
        for (String linkId : otherNodeLinkIds) {
            nextIndex.put(linkId, 0);
        }
    }

    public Address getTimerAddress() {
        return timerAddress;
    }

    public Address getGraphAddress() {
        return graphAddress;
    }

    public Address getLogAddress() {
        return logAddress;
    }

    public Random getRandom() {
        return random;
    }
    
    public String nextRandomId() {
        long ret = ((long) random.nextInt()) << 32L | (long) counter;
        counter++;
        return "" + ret;
    }

    public int nextElectionTimeout() {
        return randBetween(MIN_ELECTION_TIMEOUT, MAX_ELECTION_TIMEOUT);
    }
    
    public int getMinimumElectionTimeout() {
        return MIN_ELECTION_TIMEOUT;
    }
            
    private int randBetween(int start, int end) {
        return random.nextInt(end - start) + start;
    }

    public String getSelfLinkId() {
        return selfLinkId;
    }

    public UnmodifiableSet<String> getOtherNodeLinkIds() {
        return otherNodeLinkIds;
    }

    public int getMajorityCount() {
        int totalCount = otherNodeLinkIds.size() + 1; // + 1 because we're including ourself in the count
        int requiredSuccessfulCount = (totalCount / 2) + 1; // more than half, e.g. if 6 then (6/2)+1=4 ... e.g. if 7 then (7/2)+1=4
        
        return requiredSuccessfulCount;
    }
    
    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        Validate.notNull(mode);
        this.mode = mode;
    }
    
    public int getCurrentTerm() {
        return currentTerm;
    }

    // if true, term > currentTerm and now currentTerm = term ...
    //
    // matches up for "Rules for Servers" under "All Servers" subheading...
    //    If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to follower (§5.1)
    public boolean updateCurrentTerm(int term) {
        if (term > currentTerm) {
            currentTerm = term;
            return true;
        }
        
        return false;
    }
    
    public int incrementCurrentTerm() {
        currentTerm++;
        return currentTerm;
    }

    public String getVotedForLinkId() {
        return votedForLinkId;
    }

    public void setVotedForLinkId(String votedForLinkId) {
        this.votedForLinkId = votedForLinkId; // can be null
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public boolean containsLogEntry(int idx) {
        idx--; // log starts at idx 1, e.g. when idx=1 log[0] should be returned
        Validate.isTrue(idx >= 0);
        return idx < log.size();
    }

    public LogEntry getLogEntry(int idx) {
        idx--; // log starts at idx 1, e.g. when idx=1 log[0] should be returned
        Validate.isTrue(idx >= 0 && idx < log.size());
        return log.get(idx);
    }

    public List<LogEntry> getTailLogEntries(int fromIdx) {
        fromIdx--; // log starts at idx 1, e.g. when idx=1 log[0] should be starting pt
        Validate.isTrue(fromIdx >= 0 && fromIdx < log.size());
        return new LinkedList<>(log.subList(fromIdx, log.size()));
    }

    public LogEntry getLastLogEntry() {
        Validate.isTrue(!log.isEmpty());
        return log.get(log.size() - 1);
    }

    public int getLastLogIndex() {
        Validate.isTrue(!log.isEmpty());
        return log.size(); // this is correct, don't subtract 1 because index starts at 1
    }
    
    public void truncateLogEntries(int fromIdx) {
        fromIdx--; // log starts at idx 1, e.g. when idx=1 log[0] should be starting pt
        Validate.isTrue(fromIdx >= 0);
        while (log.size() > fromIdx) {
            log.removeLast();
        }
    }

    public void addLogEntries(List<LogEntry> newLogEntries) {
        Validate.notNull(newLogEntries);
        Validate.noNullElements(newLogEntries);
        log.addAll(newLogEntries);
    }
    
    public boolean isLogEmpty() {
        return log.isEmpty();
    }

    public int getNextIndex(String linkId) {
        Validate.notNull(linkId);
        Integer ret = nextIndex.get(linkId);
        Validate.validState(ret != null);
        return ret;
    }

    public void setNextIndex(String linkId, int value) {
        Validate.notNull(linkId);
        Validate.isTrue(nextIndex.containsKey(linkId));
        nextIndex.put(linkId, value);
    }

    public void decrementNextIndex(String linkId) {
        Validate.notNull(linkId);
        Validate.isTrue(nextIndex.containsKey(linkId));
        int idx = nextIndex.get(linkId);
        Validate.isTrue(idx > 0); // can't decrement in to negatives, min has to be 0
        idx--;
        nextIndex.put(linkId, idx);
        
    }

    public int getMatchIndex(String linkId) {
        Validate.notNull(linkId);
        Integer ret = matchIndex.get(linkId);
        Validate.validState(ret != null);
        return ret;
    }

    public void setMatchIndex(String linkId, int value) {
        Validate.notNull(linkId);
        Validate.isTrue(matchIndex.containsKey(linkId));
        matchIndex.put(linkId, value);
    }
    
}
