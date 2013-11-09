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
import java.util.concurrent.ArrayBlockingQueue;
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
        final ArrayBlockingQueue<byte[]> exchanger = new ArrayBlockingQueue<>(1); // exchanger/synchronousqueue shouldn't be used here due
                                                                                  // to potential of recvHandler getting blocked
        
        MessageReceiver<InetSocketAddress> recvHandler = new MessageReceiver<InetSocketAddress>() {

            @Override
            public boolean messageArrived(IncomingData<InetSocketAddress> packet) {
                ByteBuffer recvData = packet.getData();
                PacketId incomingPid = PacketId.extractPrependedId(recvData);
                
                if (incomingPid.equals(pid)) {
                    byte[] recvDataWithoutPid = PacketId.removePrependedId(recvData);
                    exchanger.add(recvDataWithoutPid);
                    return true;
                }
                
                return false;
            }
        };
        
        notifier.add(recvHandler);
        
        byte []sendData = pid.prependId(data);
        OutgoingData<InetSocketAddress> outgoingPacket = new OutgoingData<>(to, sendData);
        querier.sendMessage(outgoingPacket);
        
        try {
            byte[] recvData = exchanger.poll(timeout, TimeUnit.MILLISECONDS);
            
            if (recvData == null) {
                throw new IOException("Communcation failed");
            }
            
            return recvData;
        } finally {
            notifier.remove(recvHandler);
        }
    }
}
