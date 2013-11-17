package com.offbynull.p2prpc.transport.udp;

import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class EventResponseTimedOut implements Event {
    private OutgoingMessageResponseListener<InetSocketAddress> receiver;

    EventResponseTimedOut(OutgoingMessageResponseListener<InetSocketAddress> receiver) {
        Validate.notNull(receiver);
        
        this.receiver = receiver;
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getReceiver() {
        return receiver;
    }
    
    
}
