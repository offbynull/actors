package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.actor.Endpoint;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

final class HubToNodeEndpoint<A> implements Endpoint {
    private Endpoint nodeEndpoint;

    public HubToNodeEndpoint(Endpoint nodeEndpoint) {
        Validate.notNull(nodeEndpoint);
        this.nodeEndpoint = nodeEndpoint;
    }

    public Endpoint getNodeEndpoint() {
        return nodeEndpoint;
    }

    @Override
    public void send(Endpoint source, Object message) {
        nodeEndpoint.send(source, message);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.nodeEndpoint);
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
        final HubToNodeEndpoint<?> other = (HubToNodeEndpoint<?>) obj;
        if (!Objects.equals(this.nodeEndpoint, other.nodeEndpoint)) {
            return false;
        }
        return true;
    }

}
