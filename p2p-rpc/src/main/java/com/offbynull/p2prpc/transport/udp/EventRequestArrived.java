package com.offbynull.p2prpc.transport.udp;

import com.offbynull.p2prpc.transport.IncomingMessage;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import org.apache.commons.lang3.Validate;

final class EventRequestArrived implements Event {
    private IncomingMessage<InetSocketAddress> request;
    private Selector selector;
    private MessageId id;

    EventRequestArrived(IncomingMessage<InetSocketAddress> request, Selector selector, MessageId id) {
        Validate.notNull(request);
        Validate.notNull(selector);
        Validate.notNull(id);
        
        this.request = request;
        this.selector = selector;
        this.id = id;
    }

    public IncomingMessage<InetSocketAddress> getRequest() {
        return request;
    }

    public Selector getSelector() {
        return selector;
    }

    public MessageId getId() {
        return id;
    }

    
}
