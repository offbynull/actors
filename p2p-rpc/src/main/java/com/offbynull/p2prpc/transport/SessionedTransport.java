package com.offbynull.p2prpc.transport;

import java.io.IOException;

/**
 * Interface for transports that are sessioned (e.g. TCP).
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
        boolean requestArrived(IncomingData<A> data, ResponseSender<A> responseSender);
    };

    public interface ResponseSender<A> {
        void sendResponse(OutgoingData<A> data);
        void killConnection();
    };

    public interface RequestSender<A> {
        RequestController sendRequest(OutgoingData<A> data, ResponseReceiver<A> responseReceiver);
    }
    
    public interface RequestController {
        void killCommunication();
    }
    
    public interface ResponseReceiver<A> {
        void responseArrived(IncomingData<A> data);
        void communicationFailed();
    }
}
