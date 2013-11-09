package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.IncomingData;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

final class IncomingRequest {
    private IncomingData<InetSocketAddress> request;
    private Selector selector;
    private SocketChannel channel;

    IncomingRequest(IncomingData<InetSocketAddress> request, Selector selector, SocketChannel channel) {
        this.request = request;
        this.selector = selector;
        this.channel = channel;
    }

    public IncomingData<InetSocketAddress> getRequest() {
        return request;
    }

    public Selector getSelector() {
        return selector;
    }

    public SocketChannel getChannel() {
        return channel;
    }
    
}
