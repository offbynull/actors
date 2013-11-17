package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.IncomingMessageResponseHandler;
import com.offbynull.p2prpc.transport.OutgoingResponse;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class TcpIncomingMessageResponseHandler implements IncomingMessageResponseHandler {
    private LinkedBlockingQueue<Command> commandQueue;
    private Selector selector;
    private SocketChannel channel;

    TcpIncomingMessageResponseHandler(LinkedBlockingQueue<Command> commandQueue, Selector selector, SocketChannel channel) {
        Validate.notNull(commandQueue);
        Validate.notNull(selector);
        Validate.notNull(channel);
        
        this.commandQueue = commandQueue;
        this.selector = selector;
        this.channel = channel;
    }

    @Override
    public void responseReady(OutgoingResponse response) {
        Validate.notNull(response);
        
        commandQueue.add(new CommandSendResponse(channel, response));
        selector.wakeup();
    }

    @Override
    public void terminate() {
        commandQueue.add(new CommandKillEstablished(channel));
        selector.wakeup();
    }
}
