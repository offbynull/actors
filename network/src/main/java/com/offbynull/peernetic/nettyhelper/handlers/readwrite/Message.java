package com.offbynull.peernetic.nettyhelper.handlers.readwrite;

import java.net.SocketAddress;

public final class Message {
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private Object message;

    public Message(SocketAddress localAddress, SocketAddress remoteAddress, Object message) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.message = message;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public Object getMessage() {
        return message;
    }
    
}
