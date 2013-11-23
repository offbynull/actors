package com.offbynull.rpc.transport.tcp;

import com.offbynull.rpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class EventResponseErrored implements Event {
    private Throwable error;
    private OutgoingMessageResponseListener<InetSocketAddress> receiver;

    EventResponseErrored(Throwable error, OutgoingMessageResponseListener<InetSocketAddress> receiver) {
        Validate.notNull(error);
        Validate.notNull(receiver);
        
        this.error = error;
        this.receiver = receiver;
    }

    public Throwable getError() {
        return error;
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getReceiver() {
        return receiver;
    }
    
    
}
