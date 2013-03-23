package com.offbynull.peernetic.chord.messages;

import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import javax.validation.Valid;

public final class GetPredecessorResponse implements Response {
    private NodePointer predecessor;

    @Valid
    public NodePointer getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(NodePointer predecessor) {
        this.predecessor = predecessor;
    }
}
