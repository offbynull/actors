package com.offbynull.peernetic.overlay.chord.messages;

import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

public final class SetPredecessor<A> {
    private Pointer<A> predecessor;

    public SetPredecessor(Pointer<A> predecessor) {
        Validate.notNull(predecessor);

        this.predecessor = predecessor;
    }

    public Pointer<A> getPredecessor() {
        return predecessor;
    }
    
}
