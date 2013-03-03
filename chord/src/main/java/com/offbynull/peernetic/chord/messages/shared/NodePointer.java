package com.offbynull.peernetic.chord.messages.shared;

import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public final class NodePointer {
    private NodeId id;
    private NodeAddress address;

    public NodePointer() {
    }

    @NotNull
    @Valid
    public NodeId getId() {
        return id;
    }

    public void setId(NodeId id) {
        this.id = id;
    }

    @NotNull
    @Valid
    public NodeAddress getAddress() {
        return address;
    }

    public void setAddress(NodeAddress address) {
        this.address = address;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.address);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodePointer other = (NodePointer) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }
}
