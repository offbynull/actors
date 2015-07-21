package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class RequestVoteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int term;
    // candidateId not required because link id will be available once this msg comes in
    private final int lastLogIndex;
    private final int lastLogTerm;

    public RequestVoteRequest(int term, int lastLogIndex, int lastLogTerm) {
        Validate.isTrue(term >= 0);
        Validate.isTrue(lastLogIndex >= 0);
        Validate.isTrue(lastLogTerm >= 0);
        this.term = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public int getTerm() {
        return term;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }
}
