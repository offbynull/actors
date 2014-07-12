package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.network.Gateway;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class GatewayEndpointDirectory implements EndpointDirectory<InetSocketAddress> {
    private Gateway<InetSocketAddress> gateway;

    public GatewayEndpointDirectory(Gateway<InetSocketAddress> gateway) {
        Validate.notNull(gateway);
        this.gateway = gateway;
    }
    
    @Override
    public Endpoint lookup(InetSocketAddress id) {
        return new GatewayOutputEndpoint(gateway, id);
    }
    
}
