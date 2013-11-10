package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestController;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestSender;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseReceiver;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.Validate;

final class TcpRequestSender implements RequestSender<InetSocketAddress> {
    private Selector selector;
    private LinkedBlockingQueue<OutgoingRequest> outgoingData;
    private AtomicLong nextId;

    TcpRequestSender(Selector selector) {
        Validate.notNull(selector);
        
        this.selector = selector;
        this.outgoingData = new LinkedBlockingQueue<>();
        nextId = new AtomicLong();
    }

    @Override
    public RequestController sendRequest(OutgoingData<InetSocketAddress> data, ResponseReceiver<InetSocketAddress> receiver) {
        Validate.notNull(data);
        Validate.notNull(receiver);
        
        // TO FIX THIS FUNCTION...
        // assign an unique id to outgoingdata
        // pass that uniqueid to tcprequestcontroller
        long id = nextId.incrementAndGet();
        outgoingData.add(new SendQueuedRequest(data, receiver, id));
        selector.wakeup();
        return new TcpRequestController(id, selector, outgoingData);
    }

    void drainTo(Collection<OutgoingRequest> destination) {
        outgoingData.drainTo(destination);
    }
    
}
