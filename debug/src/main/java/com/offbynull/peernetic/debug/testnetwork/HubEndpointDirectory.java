package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import org.apache.commons.lang3.Validate;

public final class HubEndpointDirectory<A> implements EndpointDirectory<A> {
    
    private final A srcId;
    private final Endpoint hubEndpoint;

    public HubEndpointDirectory(A srcId, Endpoint hubEndpoint) {
        Validate.notNull(srcId);
        Validate.notNull(hubEndpoint);
        this.srcId = srcId;
        this.hubEndpoint = hubEndpoint;
    }
    
    @Override
    public Endpoint lookup(A id) {
        return new NodeToHubEndpoint<>(hubEndpoint, srcId, id);
    }
    
}
