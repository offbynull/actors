package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.NonSessionedTransport;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageReceiver;
import com.offbynull.p2prpc.transport.NonSessionedTransport.ReceiveNotifier;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageSender;
import com.offbynull.p2prpc.transport.OutgoingData;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class NonSessionedServer<A> implements Server<A> {

    private MessageSender<A> querier;
    private ReceiveNotifier<A> notifier;
    private ServerMessageCallback<A> callback;
    private long timeout;
    
    private UdpPacketTranslator udpPacketTranslator;

    public NonSessionedServer(NonSessionedTransport<A> transport, long timeout) {
        querier = transport.getMessageSender();
        notifier = transport.getReceiveNotifier();
        this.timeout = timeout;
    }

    @Override
    public void start(ServerMessageCallback<A> callback) throws IOException {
        this.callback = callback;
        udpPacketTranslator = new UdpPacketTranslator();
        
        notifier.add(udpPacketTranslator);
    }

    @Override
    public void stop() throws IOException {
        notifier.remove(udpPacketTranslator);
    }

    private final class UdpPacketTranslator implements MessageReceiver<A> {

        @Override
        public boolean messageArrived(IncomingData<A> packet) {
            A from = packet.getFrom();
            ByteBuffer recvData = packet.getData();
            
            PacketId pid = PacketId.extractPrependedId(recvData);
            byte[] data = PacketId.removePrependedId(recvData);
            
            long time = System.currentTimeMillis();
            callback.messageArrived(packet.getFrom(), data, new ResponseCallback(time, pid, from));
            
            return false; // return false here, because we have no way of knowing if this is a server request or a response to a client
                          // that wrapped the same UdpBase
        }
    }
    
    private final class ResponseCallback implements ServerResponseCallback {

        private PacketId packetId;
        private A requester;
        private long savedTime;

        public ResponseCallback(long time, PacketId packetId, A requester) {
            this.requester = requester;
            this.packetId = packetId;
            this.savedTime = time;
        }

        @Override
        public void responseReady(byte[] data) {
            long time = System.currentTimeMillis();
            if (time - savedTime < timeout) {
                byte[] dataWithPid = packetId.prependId(data);
                OutgoingData<A> outgoingPacket = new OutgoingData<>(requester, dataWithPid);
                querier.sendMessage(outgoingPacket);
            }
        }

        @Override
        public void terminate() {
            // Do nothing
        }
    }
}
