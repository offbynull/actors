package com.offbynull.p2prpc.transport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public final class UdpServer implements Server<SocketAddress> {

    private UdpBase.UdpSendQuerier querier;
    private UdpBase.UdpReceiveNotifier notifier;
    private ServerMessageCallback<SocketAddress> callback;
    private long timeout;
    
    private UdpPacketTranslator udpPacketTranslator;

    public UdpServer(UdpBase base) {
        querier = base.getSendQuerier();
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

    private final class UdpPacketTranslator implements UdpBase.UdpReceiveHandler {

        @Override
        public boolean incoming(UdpBase.UdpIncomingPacket packet) {
            SocketAddress from = packet.getFrom();
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
        private SocketAddress requester;
        private long savedTime;

        public ResponseCallback(long time, PacketId packetId, SocketAddress requester) {
            this.requester = requester;
            this.packetId = packetId;
            this.savedTime = time;
        }

        @Override
        public void responseReady(byte[] data) {
            long time = System.currentTimeMillis();
            if (time - savedTime < timeout) {
                byte[] dataWithPid = packetId.prependId(data);
                querier.send(requester, dataWithPid);
            }
        }

        @Override
        public void terminate() {
            // Do nothing
        }
    }
}
