package com.offbynull.peernetic.common.concurrent.actor.endpoints.local;

import com.offbynull.peernetic.common.concurrent.actor.EndpointHandler;
import com.offbynull.peernetic.common.concurrent.actor.Outgoing;
import java.util.Collection;

public final class LocalEndpointHandler implements EndpointHandler<LocalEndpoint> {

    @Override
    public void push(LocalEndpoint endpoint, Collection<Outgoing> outgoing) {
        endpoint.getActorQueueWriter().push(outgoing);
    }
    
}
