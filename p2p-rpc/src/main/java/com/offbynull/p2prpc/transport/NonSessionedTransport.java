package com.offbynull.p2prpc.transport;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for transports that are non-sessioned (e.g. UDP).
 * @param <A> address type
 */
public interface NonSessionedTransport<A> {
    void start() throws IOException;
    void stop() throws IOException;
    ReceiveNotifier<A> getReceiveNotifier();
    PacketSender<A> getPacketSender();
    
    public interface ReceiveNotifier<A> {
        void add(PacketReceiver<A> e);
        void remove(PacketReceiver<A> e);
    };
    
    public interface PacketSender<A> {
        void sendPacket(OutgoingData<A> data);
    }
    
    public interface PacketReceiver<A> {
        boolean packetArrived(IncomingData<A> data);
    }
}
