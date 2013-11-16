package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.NonSessionedTransport;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageReceiver;
import com.offbynull.p2prpc.transport.NonSessionedTransport.ReceiveNotifier;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageSender;
import com.offbynull.p2prpc.transport.OutgoingData;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class NonSessionedServer<A> implements Server<A> {

    private MessageSender<A> querier;
    private ReceiveNotifier<A> notifier;
    private MessageListener<A> callback;
    private long timeout;
    
    private UdpPacketTranslator udpPacketTranslator;
    
    private Lock startStopLock;

    public NonSessionedServer(NonSessionedTransport<A> transport, long timeout) {
        Validate.notNull(transport);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        querier = transport.getMessageSender();
        notifier = transport.getReceiveNotifier();
        this.timeout = timeout;
        
        startStopLock = new ReentrantLock();
    }

    @Override
    public void start(MessageListener<A> callback) throws IOException {
        Validate.notNull(callback);
        
        startStopLock.lock();
        try {
            this.callback = callback;
            udpPacketTranslator = new UdpPacketTranslator();

            notifier.add(udpPacketTranslator);
        } finally {
            startStopLock.unlock();
        }
    }

    @Override
    public void stop() throws IOException {
        startStopLock.lock();
        try {
            notifier.remove(udpPacketTranslator);
        } finally {
            startStopLock.unlock();
        }
    }

    private final class UdpPacketTranslator implements MessageReceiver<A> {

        @Override
        public boolean messageArrived(IncomingData<A> packet) {
            Validate.notNull(packet);
            
            A from = packet.getFrom();
            ByteBuffer recvData = packet.getData();
            
            if (!RequestResponseMarker.isRequest(recvData)) {
                return false;
            }
            
            recvData.position(recvData.position() + 1);
            
            PacketId pid = PacketId.extractPrependedId(recvData);
            byte[] data = PacketId.removePrependedId(recvData);
            
            long time = System.currentTimeMillis();
            callback.messageArrived(packet.getFrom(), data, new ResponseCallback(time, pid, from));
            
            return false;
        }
    }
    
    private final class ResponseCallback implements ResponseHandler {

        private PacketId packetId;
        private A requester;
        private long savedTime;

        public ResponseCallback(long time, PacketId packetId, A requester) {
            Validate.notNull(packetId);
            Validate.notNull(requester);
            
            this.requester = requester;
            this.packetId = packetId;
            this.savedTime = time;
        }

        @Override
        public void responseReady(byte[] data) {
            Validate.notNull(data);
            
            long time = System.currentTimeMillis();
            if (time - savedTime < timeout) {
                byte[] ammendedData;
                ammendedData = packetId.prependId(data);
                ammendedData = RequestResponseMarker.prependResponseMarker(ammendedData);
                
                OutgoingData<A> outgoingPacket = new OutgoingData<>(requester, ammendedData);
                querier.sendMessage(outgoingPacket);
            }
        }

        @Override
        public void terminate() {
            // Do nothing
        }
    }
}
