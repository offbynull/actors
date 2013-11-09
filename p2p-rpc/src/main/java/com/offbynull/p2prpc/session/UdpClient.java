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
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UdpClient implements Client<InetSocketAddress> {

    private MessageSender querier;
    private ReceiveNotifier notifier;
    private PacketIdGenerator pidGenerator;

    public UdpClient(UdpTransport transport, PacketIdGenerator pidGenerator) {
        querier = transport.getMessageSender();
        notifier = transport.getReceiveNotifier();
        this.pidGenerator = pidGenerator;
    }

    @Override
    public byte[] send(InetSocketAddress to, byte[] data, long timeout) throws IOException, InterruptedException {
        final PacketId pid = pidGenerator.generate();
        final Exchanger<byte[]> exchanger = new Exchanger<>();
        
        MessageReceiver<InetSocketAddress> recvHandler = new MessageReceiver<InetSocketAddress>() {

            @Override
            public boolean messageArrived(IncomingData<InetSocketAddress> packet) {
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
        OutgoingData<InetSocketAddress> outgoingPacket = new OutgoingData<>(to, sendData);
        querier.sendMessage(outgoingPacket);
        
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
