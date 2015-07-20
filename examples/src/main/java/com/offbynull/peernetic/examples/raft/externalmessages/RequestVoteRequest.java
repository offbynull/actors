package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;

public final class RequestVoteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int term;

    public RequestVoteRequest(int term) {
        this.term = term;
    }

    public int getTerm() {
        return term;
    }
    
}
