package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.IncomingMessage;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

final class EventRequestArrived implements Event {
    private IncomingMessage<InetSocketAddress> request;
    private Selector selector;
    private SocketChannel channel;

    EventRequestArrived(IncomingMessage<InetSocketAddress> request, Selector selector, SocketChannel channel) {
        Validate.notNull(request);
        Validate.notNull(selector);
        Validate.notNull(channel);
        
        this.request = request;
        this.selector = selector;
        this.channel = channel;
    }

    public IncomingMessage<InetSocketAddress> getRequest() {
        return request;
    }

    public Selector getSelector() {
        return selector;
    }

    public SocketChannel getChannel() {
        return channel;
    }
    
}
