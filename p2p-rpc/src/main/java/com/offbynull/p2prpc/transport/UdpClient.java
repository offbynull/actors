package com.offbynull.p2prpc.transport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UdpClient implements Client<SocketAddress> {

    private UdpBase.UdpSendQuerier querier;
    private UdpBase.UdpReceiveNotifier notifier;
    private PacketIdGenerator pidGenerator;

    public UdpClient(UdpBase base, PacketIdGenerator pidGenerator) {
        querier = base.getSendQuerier();
        notifier = base.getReceiveNotifier();
        this.pidGenerator = pidGenerator;
    }

    @Override
    public byte[] send(SocketAddress to, byte[] data, long timeout) throws IOException, InterruptedException {
        final PacketId pid = pidGenerator.generate();
        final Exchanger<byte[]> exchanger = new Exchanger<>();
        
        UdpBase.UdpReceiveHandler recvHandler = new UdpBase.UdpReceiveHandler() {

            @Override
            public boolean incoming(UdpBase.UdpIncomingPacket packet) {
                ByteBuffer recvData = packet.getData();
                PacketId incomingPid = PacketId.extractPrependedId(recvData);
                
                if (incomingPid.equals(pid)) {
                    byte[] recvDataWithoutPid = PacketId.removePrependedId(recvData);
                    
                    try {
                        exchanger.exchange(recvDataWithoutPid, 0, TimeUnit.MILLISECONDS);
                        return true;
                    } catch (InterruptedException | TimeoutException te) {
                        // Do nothing. The IE will have to be handled elsewhere. The TE should be gobbled.
                    } finally {
                        notifier.remove(this);
                    }
                }
                
                return false;
            }
        };
        
        notifier.add(recvHandler);
        
        byte []sendData = pid.prependId(data);
        querier.send(to, sendData);
        
        try {
            byte[] recvData = exchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
            return recvData;
        } catch (TimeoutException te) {
            return null;
        } finally {
            notifier.remove(recvHandler);
        }
    }
}
