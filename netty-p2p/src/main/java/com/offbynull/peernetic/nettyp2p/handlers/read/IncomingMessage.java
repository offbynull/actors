package com.offbynull.peernetic.nettyp2p.handlers.read;

import java.net.SocketAddress;

public final class IncomingMessage {
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private Object message;

    public IncomingMessage(SocketAddress localAddress, SocketAddress remoteAddress, Object message) {
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
