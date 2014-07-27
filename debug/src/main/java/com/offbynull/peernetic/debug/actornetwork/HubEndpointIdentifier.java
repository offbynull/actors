package com.offbynull.peernetic.debug.actornetwork;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import org.apache.commons.lang3.Validate;

public final class HubEndpointIdentifier<A> implements EndpointIdentifier<A> {

    @Override
    public A identify(Endpoint endpoint) {
        Validate.notNull(endpoint);
        if (!(endpoint instanceof NodeToHubEndpoint)) {
            return null;
        }

        return ((NodeToHubEndpoint<A>) endpoint).getAddress();
    }
    
}
