package com.offbynull.peernetic.overlay.chord.messages;

import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

public final class Notify<A> {
    private Pointer<A> self;

    public Notify(Pointer<A> predecessor) {
        Validate.notNull(predecessor);

        this.self = predecessor;
    }

    public Pointer<A> getPredecessor() {
        return self;
    }
    
}
