package com.offbynull.peernetic.overlay.chord.messages;

import com.offbynull.peernetic.overlay.common.id.Pointer;
import org.apache.commons.lang3.Validate;

public final class GetClosestPrecedingFingerReply<A> {
    private Pointer<A> closestPredecessor;

    public GetClosestPrecedingFingerReply(Pointer<A> closestPredecessor) {
        Validate.notNull(closestPredecessor);

        this.closestPredecessor = closestPredecessor;
    }

    public Pointer<A> getClosestPredecessor() {
        return closestPredecessor;
    }

}
