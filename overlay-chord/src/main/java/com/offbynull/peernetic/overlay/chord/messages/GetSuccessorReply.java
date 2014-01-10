package com.offbynull.peernetic.overlay.chord.messages;

import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

public final class GetSuccessorReply<A> {
    private Pointer<A> successor;

    public GetSuccessorReply(Pointer<A> successor) {
        Validate.notNull(successor);

        this.successor = successor;
    }

    public Pointer<A> getSuccessor() {
        return successor;
    }
    
}
