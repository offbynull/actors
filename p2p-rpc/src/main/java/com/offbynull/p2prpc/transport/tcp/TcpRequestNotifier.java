package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class TcpRequestNotifier implements SessionedTransport.RequestNotifier<InetSocketAddress> {

    private LinkedBlockingQueue<SessionedTransport.RequestReceiver> handlers;
    private LinkedBlockingQueue<Command> commandQueue;

    TcpRequestNotifier(LinkedBlockingQueue<Command> commandQueue) {
        handlers = new LinkedBlockingQueue<>();
        this.commandQueue = commandQueue;
    }

    @Override
    public void add(SessionedTransport.RequestReceiver<InetSocketAddress> e) {
        Validate.notNull(e);
        handlers.add(e);
    }

    @Override
    public void remove(SessionedTransport.RequestReceiver<InetSocketAddress> e) {
        Validate.notNull(e);
        handlers.remove(e);
    }

    void notify(Collection<Event> events) {
        Validate.noNullElements(events);

        SessionedTransport.RequestReceiver[] handlersArray = handlers.toArray(new SessionedTransport.RequestReceiver[0]);
        for (Event event : events) {
            if (event instanceof EventRequestArrived) {
                EventRequestArrived request = (EventRequestArrived) event;
                
                for (SessionedTransport.RequestReceiver<InetSocketAddress> handler : handlersArray) {
                    IncomingData<InetSocketAddress> data = request.getRequest();
                    Selector selector = request.getSelector();
                    SocketChannel channel = request.getChannel();
                    
                    TcpResponseSender responseSender = new TcpResponseSender(commandQueue, selector, channel);
                    TcpLinkController controller = new TcpLinkController(channel, selector, commandQueue);

                    try {
                        if (handler.requestArrived(data, responseSender, controller)) {
                            break;
                        }
                    } catch (RuntimeException re) {
                        controller.kill();
                    }
                }
            } else if (event instanceof EventLinkEstablished) {
                EventLinkEstablished newLink = (EventLinkEstablished) event;

                for (SessionedTransport.RequestReceiver<InetSocketAddress> handler : handlersArray) {
                    InetSocketAddress from = newLink.getFrom();
                    SocketChannel channel = newLink.getChannel();
                    Selector selector = newLink.getSelector();

                    TcpLinkController controller = new TcpLinkController(channel, selector, commandQueue);

                    try {
                        if (handler.linkEstablished(from, controller)) {
                            break;
                        }
                    } catch (RuntimeException re) {
                        controller.kill();
                    }
                }
            }
        }
    }
}
