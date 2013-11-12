package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class TcpResponseSender implements SessionedTransport.ResponseSender<InetSocketAddress> {
    private LinkedBlockingQueue<Command> commandQueue;
    private Selector selector;
    private SocketChannel channel;

    TcpResponseSender(LinkedBlockingQueue<Command> commandQueue, Selector selector, SocketChannel channel) {
        Validate.notNull(commandQueue);
        Validate.notNull(selector);
        Validate.notNull(channel);
        
        this.commandQueue = commandQueue;
        this.selector = selector;
        this.channel = channel;
    }

    @Override
    public void sendResponse(OutgoingData<InetSocketAddress> data) {
        Validate.notNull(data);
        
        commandQueue.add(new CommandSendResponse(channel, data));
        selector.wakeup();
    }
}
