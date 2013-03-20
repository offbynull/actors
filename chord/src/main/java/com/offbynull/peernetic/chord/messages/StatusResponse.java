package com.offbynull.peernetic.chord.messages;

import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.validation.NotNullSetElements;
import java.util.Objects;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public final class StatusResponse implements Response {
    private NodeId id;
    private Set<NodePointer> pointers;

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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.pointers);
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
        final StatusResponse other = (StatusResponse) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.pointers, other.pointers)) {
            return false;
        }
        return true;
    }
}
