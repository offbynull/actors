package com.offbynull.p2prpc.transport.tcp;

import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

public final class EventLinkEstablished implements Event {
    private InetSocketAddress from;
    private Selector selector;
    private SocketChannel channel;

    EventLinkEstablished(InetSocketAddress from, Selector selector, SocketChannel channel) {
        Validate.notNull(from);
        Validate.notNull(selector);
        Validate.notNull(channel);
        
        this.from = from;
        this.selector = selector;
        this.channel = channel;
    }

    public InetSocketAddress getFrom() {
        return from;
    }

    public Selector getSelector() {
        return selector;
    }

    public SocketChannel getChannel() {
        return channel;
    }
}
