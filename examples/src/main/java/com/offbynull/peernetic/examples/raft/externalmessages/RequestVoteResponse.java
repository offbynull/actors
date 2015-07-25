package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class RequestVoteResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int term;
    private final boolean voteGranted;

    public RequestVoteResponse(int term, boolean voteGranted) {
        Validate.isTrue(term >= 0);
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }


}