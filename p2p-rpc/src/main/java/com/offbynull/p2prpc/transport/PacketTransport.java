package com.offbynull.p2prpc.transport;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for packet-oriented transports where there is no session or reliability (e.g. UDP). There should be no built-in way of linking
 * a request and response together. To do something like that, you need to add in that extra data and the logic to understand it at a higher
 * layer.
 * @param <A> address type
 */
public interface PacketTransport<A> {
    void start() throws IOException;
    void stop() throws IOException;
    ReceiveNotifier<A> getReceiveNotifier();
    PacketSender<A> getPacketSender();
    
    public interface ReceiveNotifier<A> {
        void add(PacketReceiver<A> e);
        void remove(PacketReceiver<A> e);
        // these don't need to be exposed
//        void notify(IncomingPacket<A> ... packets);
//        void notify(Collection<IncomingPacket<A>> packets);
    };
    
    public interface PacketSender<A> {
        void sendPacket(OutgoingPacket<A> packet);
    }
    
    public interface PacketReceiver<A> {
        boolean packetArrived(IncomingPacket<A> packet);
    }
    
    public static final class IncomingPacket<A> {
        private A from;
        private ByteBuffer data;
        private long arriveTime;

        public IncomingPacket(A from, byte[] data, long arriveTime) {
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
    
    public static final class OutgoingPacket<A> {
        private A to;
        private ByteBuffer data;

        public OutgoingPacket(A to, byte[] data) {
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
