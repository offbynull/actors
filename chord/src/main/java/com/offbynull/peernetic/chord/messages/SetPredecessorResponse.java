package com.offbynull.peernetic.chord.messages;

import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public final class SetPredecessorResponse implements Response {
    private NodePointer assignedPredecessor;

    @NotNull
    @Valid
    public NodePointer getAssignedPredecessor() {
        return assignedPredecessor;
    }

    public void setAssignedPredecessor(NodePointer assignedPredecessor) {
        this.assignedPredecessor = assignedPredecessor;
    }
}
