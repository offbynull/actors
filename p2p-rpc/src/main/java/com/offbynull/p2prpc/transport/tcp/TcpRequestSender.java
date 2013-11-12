package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport.LinkController;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestSender;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseReceiver;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.Validate;

final class TcpRequestSender implements RequestSender<InetSocketAddress> {
    private Selector selector;
    private LinkedBlockingQueue<Command> commandQueue;
    private AtomicLong nextId;

    TcpRequestSender(Selector selector, LinkedBlockingQueue<Command> commandQueue) {
        Validate.notNull(selector);
        
        this.selector = selector;
        this.commandQueue = commandQueue;
        nextId = new AtomicLong();
    }

    @Override
    public LinkController sendRequest(OutgoingData<InetSocketAddress> data, ResponseReceiver<InetSocketAddress> receiver) {
        Validate.notNull(data);
        Validate.notNull(receiver);
        
        // TO FIX THIS FUNCTION...
        // assign an unique id to outgoingdata
        // pass that uniqueid to tcprequestcontroller
        long id = nextId.incrementAndGet();
        commandQueue.add(new CommandSendRequest(data, receiver, id));
        selector.wakeup();
        
        /*
        Don't need to worry about race conditions or anything like that by passing the id. The CommandSendRequest will always happen before
        any kill request by the link controller. It was added in above, but for the kill command to be put in to the queue, the user has to
        trigger the link controller being returned below -- which will add a kill command on to the queue.
        */
        
        return new TcpLinkController(id, selector, commandQueue);
    }
}
