package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;

public final class RequestVoteResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final boolean success;

    public RequestVoteResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

}
