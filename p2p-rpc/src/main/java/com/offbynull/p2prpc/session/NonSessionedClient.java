package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingMessage;
import com.offbynull.p2prpc.transport.NonSessionedTransport;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageReceiver;
import com.offbynull.p2prpc.transport.NonSessionedTransport.ReceiveNotifier;
import com.offbynull.p2prpc.transport.NonSessionedTransport.MessageSender;
import com.offbynull.p2prpc.transport.OutgoingMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public final class NonSessionedClient<A> implements Client<A> {

    private MessageSender<A> querier;
    private ReceiveNotifier<A> notifier;
    private MessageIdGenerator pidGenerator;

    public NonSessionedClient(NonSessionedTransport<A> transport, MessageIdGenerator pidGenerator) {
        Validate.notNull(transport);
        Validate.notNull(pidGenerator);
        
        querier = transport.getMessageSender();
        notifier = transport.getReceiveNotifier();
        this.pidGenerator = pidGenerator;
    }

    @Override
    public byte[] send(A to, byte[] data, long timeout) throws IOException, InterruptedException {
        Validate.notNull(to);
        Validate.notNull(data);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        final MessageId pid = pidGenerator.generate();
        final ArrayBlockingQueue<byte[]> exchanger = new ArrayBlockingQueue<>(1); // exchanger/synchronousqueue shouldn't be used here due
                                                                                  // to potential of recvHandler getting blocked
        
        MessageReceiver<A> recvHandler = new MessageReceiver<A>() {

            @Override
            public boolean messageArrived(IncomingMessage<A> packet) {
                ByteBuffer recvData = packet.getData();
                
                if (!RequestResponseMarker.isResponse(recvData)) {
                    return false;
                }
                
                recvData.position(recvData.position() + 1);
                MessageId incomingPid = MessageId.extractPrependedId(recvData);
                
                if (!incomingPid.equals(pid)) {
                    return false;
                }
                
                byte[] recvDataWithoutPid = MessageId.removePrependedId(recvData);
                exchanger.add(recvDataWithoutPid);
                return true;
            }
        };
        
        notifier.add(recvHandler);
        
        byte []sendData;
        sendData = pid.prependId(data);
        sendData = RequestResponseMarker.prependRequestMarker(sendData);
        OutgoingMessage<A> outgoingPacket = new OutgoingMessage<>(to, sendData);
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
