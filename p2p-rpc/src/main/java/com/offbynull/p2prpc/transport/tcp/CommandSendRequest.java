package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingMessage;
import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class CommandSendRequest implements Command {
    private OutgoingMessage<InetSocketAddress> message;
    private OutgoingMessageResponseListener<InetSocketAddress> responseListener;

    CommandSendRequest(OutgoingMessage<InetSocketAddress> message, OutgoingMessageResponseListener<InetSocketAddress> responseListener) {
        Validate.notNull(message);
        Validate.notNull(responseListener);
        
        this.message = message;
        this.responseListener = responseListener;
    }

    public OutgoingMessage<InetSocketAddress> getMessage() {
        return message;
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getResponseListener() {
        return responseListener;
    }

}
