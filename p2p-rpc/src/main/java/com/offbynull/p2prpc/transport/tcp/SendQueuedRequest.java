package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import com.offbynull.p2prpc.transport.StreamIoBuffers;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class SendQueuedRequest implements OutgoingRequest {
    private InetSocketAddress destination;
    private StreamIoBuffers buffers;
    private SessionedTransport.ResponseReceiver<InetSocketAddress> receiver;
    private long id;

    SendQueuedRequest(OutgoingData<InetSocketAddress> data, SessionedTransport.ResponseReceiver<InetSocketAddress> receiver, long id) {
        Validate.notNull(data);
        Validate.notNull(receiver);
        
        this.destination = data.getTo();
        StreamIoBuffers streamIoBuffers = new StreamIoBuffers(StreamIoBuffers.Mode.WRITE_FIRST);
        streamIoBuffers.startWriting(data.getData());
        this.buffers = streamIoBuffers;
        this.receiver = receiver;
        this.id = id;
    }

    public InetSocketAddress getDestination() {
        return destination;
    }

    public StreamIoBuffers getBuffers() {
        return buffers;
    }

    public SessionedTransport.ResponseReceiver<InetSocketAddress> getReceiver() {
        return receiver;
    }

    public long getId() {
        return id;
    }
    
}
