package com.offbynull.p2prpc.transport;

import java.io.IOException;

/**
 * Interface for transports that are non-sessioned and may be unreliable (e.g. UDP). Implementations must be thread-safe.
 * @param <A> address type
 */
public interface NonSessionedTransport<A> {
    void start() throws IOException;
    void stop() throws IOException;
    ReceiveNotifier<A> getReceiveNotifier();
    MessageSender<A> getMessageSender();
    
    public interface ReceiveNotifier<A> {
        void add(MessageReceiver<A> e);
        void remove(MessageReceiver<A> e);
    };
    
    public interface MessageSender<A> {
        void sendMessage(OutgoingData<A> data);
    }
    
    public interface MessageReceiver<A> {
        boolean messageArrived(IncomingData<A> data);
    }
}
