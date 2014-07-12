package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class GatewayEndpointIdentifier implements EndpointIdentifier<InetSocketAddress> {

    @Override
    public InetSocketAddress identify(Endpoint endpoint) {
        Validate.notNull(endpoint);
        if (!(endpoint instanceof GatewayOutputEndpoint)) {
            return null;
        }

        return ((GatewayOutputEndpoint) endpoint).getAddress();
    }
    
}
