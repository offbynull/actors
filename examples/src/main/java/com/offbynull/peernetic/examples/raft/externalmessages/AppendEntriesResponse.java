package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class AppendEntriesResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int term;
    private final boolean success;

    public AppendEntriesResponse(int term, boolean success) {
        Validate.isTrue(term >= 0);
        this.term = term;
        this.success = success;
    }

    public int getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

}
