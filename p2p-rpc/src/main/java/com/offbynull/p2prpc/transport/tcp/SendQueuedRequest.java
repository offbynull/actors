package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class SendQueuedRequest implements OutgoingRequest {
    private InetSocketAddress destination;
    private ByteBuffer data;
    private SessionedTransport.ResponseReceiver<InetSocketAddress> receiver;
    private long id;

    SendQueuedRequest(OutgoingData<InetSocketAddress> data, SessionedTransport.ResponseReceiver<InetSocketAddress> receiver, long id) {
        Validate.notNull(data);
        Validate.notNull(receiver);
        
        this.destination = data.getTo();
        this.data = data.getData();
        this.receiver = receiver;
        this.id = id;
    }

    public InetSocketAddress getDestination() {
        return destination;
    }

    public ByteBuffer getData() {
        return data;
    }

    public SessionedTransport.ResponseReceiver<InetSocketAddress> getReceiver() {
        return receiver;
    }

    public long getId() {
        return id;
    }
    
}
