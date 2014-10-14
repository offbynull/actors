package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import org.apache.commons.lang3.Validate;

public final class TransmissionEndpointIdentifier<A> implements EndpointIdentifier<A> {

    @Override
    public A identify(Endpoint endpoint) {
        Validate.notNull(endpoint);
        if (!(endpoint instanceof TransmissionOutputEndpoint)) {
            return null;
        }

        return ((TransmissionOutputEndpoint<A>) endpoint).getAddress();
    }
    
}
