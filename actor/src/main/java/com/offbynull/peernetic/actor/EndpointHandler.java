package com.offbynull.peernetic.actor;

import java.util.Collection;

public interface EndpointHandler<E extends Endpoint> {
    void push(Endpoint source, E destination, Collection<Outgoing> outgoing);
}
