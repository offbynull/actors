package com.offbynull.peernetic.chord.messages;

import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.validation.NotNullSetElements;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public final class StatusResponse implements Response {
    private NodeId id;
    private Set<NodePointer> pointers;
    private NodePointer predecessor;

    public StatusResponse() {
    }

    @NotNull
    public NodeId getId() {
        return id;
    }

    public void setId(NodeId id) {
        this.id = id;
    }

    @NotNull
    @NotNullSetElements
    @NotEmpty
    @Valid
    public Set<NodePointer> getPointers() {
        return pointers;
    }

    public void setPointers(Set<NodePointer> pointers) {
        this.pointers = pointers;
    }

    @Valid
    public NodePointer getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(NodePointer predecessor) {
        this.predecessor = predecessor;
    }
    
}
