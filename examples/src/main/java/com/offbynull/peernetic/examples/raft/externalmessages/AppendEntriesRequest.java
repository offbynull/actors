package com.offbynull.peernetic.examples.raft.externalmessages;

import com.offbynull.peernetic.examples.raft.LogEntry;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class AppendEntriesRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int term;
    private final String leaderLinkId;
    private final int prevLogIndex;
    private final int prevLogTerm;
    private final UnmodifiableList<LogEntry> entries;
    private final int leaderCommit;

    public AppendEntriesRequest(int term, String leaderLinkId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries,
            int leaderCommit) {
        Validate.notNull(leaderLinkId);
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        Validate.isTrue(term >= 0);
        Validate.isTrue(prevLogIndex >= 0);
        Validate.isTrue(prevLogTerm >= 0);
        Validate.isTrue(leaderCommit >= 0);
        this.term = term;
        this.leaderLinkId = leaderLinkId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = (UnmodifiableList<LogEntry>) UnmodifiableList.unmodifiableList(new ArrayList<LogEntry>(entries));
        this.leaderCommit = leaderCommit;
    }

    public int getTerm() {
        return term;
    }

    public String getLeaderLinkId() {
        return leaderLinkId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public UnmodifiableList<LogEntry> getEntries() {
        return entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }
    
}
