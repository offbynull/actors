package com.offbynull.p2prpc.transport.tcp;

import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

final class KillQueuedResponse implements OutgoingResponse {
    private SocketChannel channel;

    KillQueuedResponse(SocketChannel channel) {
        Validate.notNull(channel);
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }
    
}
