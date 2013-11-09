package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

final class TcpRequestNotifier implements SessionedTransport.RequestNotifier<InetSocketAddress> {
    private LinkedBlockingQueue<SessionedTransport.RequestReceiver> handlers;
    private LinkedBlockingQueue<OutgoingResponse> queuedResponses;

    TcpRequestNotifier() {
        handlers = new LinkedBlockingQueue<>();
        queuedResponses = new LinkedBlockingQueue<>();
    }

    @Override
    public void add(SessionedTransport.RequestReceiver<InetSocketAddress> e) {
        handlers.add(e);
    }

    @Override
    public void remove(SessionedTransport.RequestReceiver<InetSocketAddress> e) {
        handlers.remove(e);
    }

    void notify(Collection<IncomingRequest> dataCollection) {
        SessionedTransport.RequestReceiver[] handlersArray = handlers.toArray(new SessionedTransport.RequestReceiver[0]);
        for (IncomingRequest incomingRequest : dataCollection) {
            for (SessionedTransport.RequestReceiver<InetSocketAddress> handler : handlersArray) {
                // to array to avoid locks
                IncomingData<InetSocketAddress> data = incomingRequest.getRequest();
                Selector selector = incomingRequest.getSelector();
                SocketChannel channel = incomingRequest.getChannel();
                TcpResponseSender responseSender = new TcpResponseSender(queuedResponses, selector, channel);
                if (handler.requestArrived(data, responseSender)) {
                    break;
                }
            }
        }
    }

    void drainResponsesTo(Collection<OutgoingResponse> destination) {
        queuedResponses.drainTo(destination);
    }
    
}
