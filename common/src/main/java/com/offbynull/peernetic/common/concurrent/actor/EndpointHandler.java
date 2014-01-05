package com.offbynull.peernetic.common.concurrent.actor;

import java.util.Collection;

public interface EndpointHandler<E extends Endpoint> {
    void push(Endpoint source, E destination, Collection<Outgoing> outgoing);
}
