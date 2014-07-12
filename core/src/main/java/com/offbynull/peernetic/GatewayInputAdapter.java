package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.network.Message;
import com.offbynull.peernetic.network.GatewayListener;
import org.apache.commons.lang3.Validate;

public final class GatewayInputAdapter implements GatewayListener {
    private final Endpoint endpoint;

    public GatewayInputAdapter(Endpoint endpoint) {
        Validate.notNull(endpoint);
        
        this.endpoint = endpoint;
    }
    

    @Override
    public void onReadMessage(Message incomingMessage) {
        Validate.notNull(incomingMessage);
        
        endpoint.send(
                new GatewayOutputEndpoint(incomingMessage.getGateway(), incomingMessage.getRemoteAddress()),
                incomingMessage.getMessage());
    }
}
