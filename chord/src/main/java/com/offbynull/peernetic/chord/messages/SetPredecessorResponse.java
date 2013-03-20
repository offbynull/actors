package com.offbynull.peernetic.chord.messages;

import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// returns whether or not pred was set and what the current pred is
public final class SetPredecessorResponse implements Response {
    private boolean set;
    private NodePointer predecessor;

    public boolean isSet() {
        return set;
    }

    public void setSet(boolean set) {
        this.set = set;
    }

    @NotNull
    @Valid
    public NodePointer getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(NodePointer predecessor) {
        this.predecessor = predecessor;
    }
    
}
