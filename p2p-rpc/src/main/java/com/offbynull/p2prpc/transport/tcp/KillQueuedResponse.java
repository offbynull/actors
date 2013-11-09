package com.offbynull.p2prpc.transport.tcp;

import java.nio.channels.SocketChannel;

final class KillQueuedResponse implements OutgoingResponse {
    private SocketChannel channel;

    KillQueuedResponse(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }
    
}
