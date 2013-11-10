package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

final class TcpResponseSender implements SessionedTransport.ResponseSender<InetSocketAddress> {
    private LinkedBlockingQueue<OutgoingResponse> queue;
    private Selector selector;
    private SocketChannel channel;

    TcpResponseSender(LinkedBlockingQueue<OutgoingResponse> queue, Selector selector, SocketChannel channel) {
        this.queue = queue;
        this.selector = selector;
        this.channel = channel;
    }

    @Override
    public void sendResponse(OutgoingData<InetSocketAddress> data) {
        queue.add(new SendQueuedResponse(channel, data));
        selector.wakeup();
    }

    @Override
    public void killConnection() {
        queue.add(new KillQueuedResponse(channel));
        selector.wakeup();
    }
    
}
