package com.offbynull.rpc.transport.udp;

import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.rpc.transport.OutgoingResponse;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class UdpIncomingMessageResponseHandler implements IncomingMessageResponseHandler {
    private LinkedBlockingQueue<Command> commandQueue;
    private Selector selector;
    private MessageId id;
    private InetSocketAddress address;

    UdpIncomingMessageResponseHandler(LinkedBlockingQueue<Command> commandQueue, Selector selector, MessageId id,
            InetSocketAddress address) {
        Validate.notNull(commandQueue);
        Validate.notNull(selector);
        Validate.notNull(id);
        Validate.notNull(address);
        
        this.commandQueue = commandQueue;
        this.selector = selector;
        this.id = id;
        this.address = address;
    }

    @Override
    public void responseReady(OutgoingResponse response) {
        Validate.notNull(response);
        
        commandQueue.add(new CommandSendResponse(id, address, response));
        selector.wakeup();
    }

    @Override
    public void terminate() {
        // do nothing
    }
}
