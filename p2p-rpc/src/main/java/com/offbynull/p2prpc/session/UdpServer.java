package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.PacketTransport.IncomingPacket;
import com.offbynull.p2prpc.transport.PacketTransport.OutgoingPacket;
import com.offbynull.p2prpc.transport.PacketTransport.PacketReceiver;
import com.offbynull.p2prpc.transport.PacketTransport.ReceiveNotifier;
import com.offbynull.p2prpc.transport.PacketTransport.PacketSender;
import com.offbynull.p2prpc.transport.UdpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public final class UdpServer implements Server<SocketAddress> {

    private PacketSender querier;
    private ReceiveNotifier notifier;
    private ServerMessageCallback<SocketAddress> callback;
    private long timeout;
    
    private UdpPacketTranslator udpPacketTranslator;

    public UdpServer(UdpTransport base) {
        querier = base.getPacketSender();
        notifier = base.getReceiveNotifier();
    }

    @Override
    public void start(ServerMessageCallback<SocketAddress> callback) throws IOException {
        this.callback = callback;
        udpPacketTranslator = new UdpPacketTranslator();
        
        notifier.add(udpPacketTranslator);
    }

    @Override
    public void stop() throws IOException {
        notifier.remove(udpPacketTranslator);
    }

    private final class UdpPacketTranslator implements PacketReceiver<InetSocketAddress> {

        @Override
        public boolean packetArrived(IncomingPacket<InetSocketAddress> packet) {
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
                OutgoingPacket<InetSocketAddress> outgoingPacket = new OutgoingPacket<>(requester, dataWithPid);
                querier.sendPacket(outgoingPacket);
            }
        }

        @Override
        public void terminate() {
            // Do nothing
        }
    }
}
