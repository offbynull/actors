package com.offbynull.peernetic.network;

import org.apache.commons.lang3.Validate;

public final class Message<A> {
    private A localAddress;
    private A remoteAddress;
    private Object message;
    private Gateway gateway;

    public Message(A localAddress, A remoteAddress, Object message, Gateway client) {
        Validate.notNull(localAddress);
        Validate.notNull(remoteAddress);
        Validate.notNull(message);
        Validate.notNull(client);
        
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.message = message;
        this.gateway = client;
    }

    public A getLocalAddress() {
        return localAddress;
    }

    public A getRemoteAddress() {
        return remoteAddress;
    }

    public Object getMessage() {
        return message;
    }

    public Gateway getGateway() {
        return gateway;
    }
}
