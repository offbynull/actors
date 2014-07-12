package com.offbynull.peernetic.network;

import java.net.InetSocketAddress;

public final class Message {
    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;
    private Object message;
    private Gateway gateway;

    public Message(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Object message, Gateway client) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.message = message;
        this.gateway = client;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public Object getMessage() {
        return message;
    }

    public Gateway getGateway() {
        return gateway;
    }
}
