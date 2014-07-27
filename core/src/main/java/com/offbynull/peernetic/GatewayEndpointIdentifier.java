package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import org.apache.commons.lang3.Validate;

public final class GatewayEndpointIdentifier<A> implements EndpointIdentifier<A> {

    @Override
    public A identify(Endpoint endpoint) {
        Validate.notNull(endpoint);
        if (!(endpoint instanceof GatewayOutputEndpoint)) {
            return null;
        }

        return ((GatewayOutputEndpoint<A>) endpoint).getAddress();
    }
    
}
