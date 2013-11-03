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
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UdpClient implements Client<InetSocketAddress> {

    private PacketSender querier;
    private ReceiveNotifier notifier;
    private PacketIdGenerator pidGenerator;

    public UdpClient(UdpTransport base, PacketIdGenerator pidGenerator) {
        querier = base.getPacketSender();
        notifier = base.getReceiveNotifier();
        this.pidGenerator = pidGenerator;
    }

    @Override
    public byte[] send(InetSocketAddress to, byte[] data, long timeout) throws IOException, InterruptedException {
        final PacketId pid = pidGenerator.generate();
        final Exchanger<byte[]> exchanger = new Exchanger<>();
        
        PacketReceiver<InetSocketAddress> recvHandler = new PacketReceiver<InetSocketAddress>() {

            @Override
            public boolean packetArrived(IncomingPacket<InetSocketAddress> packet) {
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
        OutgoingPacket<InetSocketAddress> outgoingPacket = new OutgoingPacket<>(to, sendData);
        querier.sendPacket(outgoingPacket);
        
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
