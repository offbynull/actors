package com.offbynull.p2prpc.transport;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for stream-oriented transports where there is some kind of session/connection and guarantee of in-order data delivery
 * (e.g. TCP). This interface is intended for short messages that need a response.
 * <p/>
 * Send example
 * -----------
 * A Connect
 * A Send message to A
 * A Send end-of-write marker to B
 * B Receive message and end-of-write marker
 * B Send response to A
 * B Send end-of-write marker
 * A Receive response and end-of-write marker from B
 * A/B Disconnect
 * 
 * @param <A> address type
 */
public interface StreamTransport<A> {
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
        void killCommunication();
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
    
    public static final class IncomingData<A> {
        private A from;
        private ByteBuffer data;
        private long arriveTime;

        public IncomingData(A from, byte[] data, long arriveTime) {
            this.from = from;
            this.data = ByteBuffer.allocate(data.length).put(data);
            this.arriveTime = arriveTime;
            
            this.data.flip();
        }

        public A getFrom() {
            return from;
        }

        public ByteBuffer getData() {
            return data.asReadOnlyBuffer();
        }

        public long getArriveTime() {
            return arriveTime;
        }
        
    }
    
    public static final class OutgoingData<A> {
        private A to;
        private ByteBuffer data;

        public OutgoingData(A to, byte[] data) {
            this.to = to;
            this.data = ByteBuffer.allocate(data.length).put(data);
            
            this.data.flip();
        }

        public A getTo() {
            return to;
        }

        public ByteBuffer getData() {
            return data.asReadOnlyBuffer();
        }
    }
}
