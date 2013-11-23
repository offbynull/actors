package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.IncomingResponse;
import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class EventResponseArrived implements Event {
    private IncomingResponse<InetSocketAddress> response;
    private OutgoingMessageResponseListener<InetSocketAddress> receiver;

    EventResponseArrived(IncomingResponse<InetSocketAddress> response, OutgoingMessageResponseListener<InetSocketAddress> receiver) {
        Validate.notNull(response);
        Validate.notNull(receiver);
        
        this.response = response;
        this.receiver = receiver;
    }

    public IncomingResponse<InetSocketAddress> getResponse() {
        return response;
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getReceiver() {
        return receiver;
    }
    
    
}
