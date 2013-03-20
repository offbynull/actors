package com.offbynull.peernetic.chord.messages;

import com.offbynull.peernetic.eventframework.impl.network.message.Request;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public final class SetPredecessorRequest implements Request {
    private NodePointer predecessor;

    @NotNull
    @Valid
    public NodePointer getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(NodePointer predecessor) {
        this.predecessor = predecessor;
    }
    
}
