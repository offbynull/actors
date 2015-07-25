package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;

// node is leader and successfully added entry to log
public final class PushEntryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int index;
    private final int term;

    public PushEntryResponse(int index, int term) {
        this.index = index;
        this.term = term;
    }

    public int getIndex() {
        return index;
    }

    public int getTerm() {
        return term;
    }
    
}
