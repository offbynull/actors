package com.offbynull.p2prpc.transport.udp;

import com.offbynull.p2prpc.transport.NonSessionedTransport;
import com.offbynull.p2prpc.transport.OutgoingMessage;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class UdpPacketSender implements NonSessionedTransport.MessageSender<InetSocketAddress> {
    private Selector selector;
    private LinkedBlockingQueue<OutgoingMessage<InetSocketAddress>> outgoingPackets;

    UdpPacketSender(Selector selector) {
        Validate.notNull(selector);
        this.selector = selector;
        this.outgoingPackets = new LinkedBlockingQueue<>();
    }

    @Override
    public void sendMessage(OutgoingMessage<InetSocketAddress> packet) {
        Validate.notNull(packet);
        outgoingPackets.add(packet);
        selector.wakeup();
    }

    void drainTo(Collection<OutgoingMessage<InetSocketAddress>> destination) {
        Validate.notNull(destination);
        outgoingPackets.drainTo(destination);
    }
    
}
