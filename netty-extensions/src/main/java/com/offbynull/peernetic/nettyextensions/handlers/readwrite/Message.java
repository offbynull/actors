package com.offbynull.peernetic.nettyextensions.handlers.readwrite;

import io.netty.channel.Channel;
import java.net.SocketAddress;

public final class Message {
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private Object message;
    private Channel channel;

    public Message(SocketAddress localAddress, SocketAddress remoteAddress, Object message, Channel channel) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.message = message;
        this.channel = channel;
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

    public Channel getChannel() {
        return channel;
    }
    
}
