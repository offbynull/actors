package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.network.Gateway;
import org.apache.commons.lang3.Validate;

public final class GatewayEndpointDirectory<A> implements EndpointDirectory<A> {
    private Gateway<A> gateway;

    public GatewayEndpointDirectory(Gateway<A> gateway) {
        Validate.notNull(gateway);
        this.gateway = gateway;
    }
    
    @Override
    public Endpoint lookup(A id) {
        return new GatewayOutputEndpoint<>(gateway, id);
    }
    
}
