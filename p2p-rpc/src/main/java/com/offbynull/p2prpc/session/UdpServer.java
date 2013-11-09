package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageReceiver;
import com.offbynull.p2prpc.transport.NonSessionedTransport.ReceiveNotifier;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageSender;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.udp.UdpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public final class UdpServer implements Server<InetSocketAddress> {

    private MessageSender querier;
    private ReceiveNotifier notifier;
    private ServerMessageCallback<InetSocketAddress> callback;
    private long timeout;
    
    private UdpPacketTranslator udpPacketTranslator;

    public UdpServer(UdpTransport transport) {
        querier = transport.getMessageSender();
        notifier = transport.getReceiveNotifier();
    }

    @Override
    public void start(ServerMessageCallback<InetSocketAddress> callback) throws IOException {
        this.callback = callback;
        udpPacketTranslator = new UdpPacketTranslator();
        
        notifier.add(udpPacketTranslator);
    }

    @Override
    public void stop() throws IOException {
        notifier.remove(udpPacketTranslator);
    }

    private final class UdpPacketTranslator implements MessageReceiver<InetSocketAddress> {

        @Override
        public boolean messageArrived(IncomingData<InetSocketAddress> packet) {
            InetSocketAddress from = packet.getFrom();
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
        private InetSocketAddress requester;
        private long savedTime;

        public ResponseCallback(long time, PacketId packetId, InetSocketAddress requester) {
            this.requester = requester;
            this.packetId = packetId;
            this.savedTime = time;
        }

        @Override
        public void responseReady(byte[] data) {
            long time = System.currentTimeMillis();
            if (time - savedTime < timeout) {
                byte[] dataWithPid = packetId.prependId(data);
                OutgoingData<InetSocketAddress> outgoingPacket = new OutgoingData<>(requester, dataWithPid);
                querier.sendMessage(outgoingPacket);
            }
        }

        @Override
        public void terminate() {
            // Do nothing
        }
    }
}
