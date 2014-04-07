package com.offbynull.peernetic.actor.network;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.Outgoing;
import com.offbynull.peernetic.networktools.netty.handlers.readwrite.IncomingMessageListener;
import com.offbynull.peernetic.networktools.netty.handlers.readwrite.Message;
import java.util.Collections;

public final class IncomingMessageToEndpointAdapter implements IncomingMessageListener {
    private volatile Endpoint endpoint = new NullEndpoint();

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void newMessage(Message incomingMessage) {
        endpoint.push(
                new NetworkEndpoint(incomingMessage.getChannel(), incomingMessage.getRemoteAddress()),
                Collections.singleton(new Outgoing(incomingMessage.getMessage(), endpoint)));
    }
}
