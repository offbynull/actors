package com.offbynull.peernetic.rpc.transport;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointFinder;
import org.apache.commons.lang3.Validate;

public final class NetworkEndpointFinder<A> implements EndpointFinder<A> {
    private Endpoint transportEndpoint;

    NetworkEndpointFinder(Endpoint transportEndpoint) {
        Validate.notNull(transportEndpoint);
        this.transportEndpoint = transportEndpoint;
    }
    

    @Override
    public Endpoint findEndpoint(A address) {
        return new NetworkEndpoint(transportEndpoint, address);
    }
}
