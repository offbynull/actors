package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import org.apache.commons.lang3.Validate;

public final class TransmissionEndpointDirectory<A> implements EndpointDirectory<A> {
    private Endpoint transActorEndpoint;

    public TransmissionEndpointDirectory(Endpoint transActorEndpoint) {
        Validate.notNull(transActorEndpoint);
        this.transActorEndpoint = transActorEndpoint;
    }
    
    @Override
    public Endpoint lookup(A address) {
        return new TransmissionOutputEndpoint<>(transActorEndpoint, address);
    }
    
}
