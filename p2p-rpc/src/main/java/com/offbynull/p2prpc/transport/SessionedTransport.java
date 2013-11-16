package com.offbynull.p2prpc.transport;

import java.io.IOException;

/**
 * Interface for transports that are sessioned (e.g. TCP). Implementations must be thread-safe.
 * @param <A> address type
 */
public interface SessionedTransport<A> {
    void start() throws IOException;
    void stop() throws IOException;
    RequestNotifier<A> getRequestNotifier();
    RequestSender<A> getRequestSender();
    
    public interface RequestNotifier<A> {
        void add(RequestReceiver<A> e);
        void remove(RequestReceiver<A> e);
    }
    
    public interface RequestReceiver<A> {
        boolean linkEstablished(A from, LinkController controller);
        boolean requestArrived(IncomingData<A> data, ResponseSender<A> responseSender, LinkController controller);
    };

    public interface ResponseSender<A> {
        void sendResponse(OutgoingData<A> data);
    };

    public interface RequestSender<A> {
        LinkController sendRequest(OutgoingData<A> data, ResponseReceiver<A> responseReceiver);
    }
    
    public interface LinkController {
        void kill();
    }
    
    public interface ResponseReceiver<A> {
        void responseArrived(IncomingData<A> data);
        void internalFailure(Throwable t);
    }
}
