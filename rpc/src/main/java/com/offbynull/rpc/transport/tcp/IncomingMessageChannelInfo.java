package com.offbynull.rpc.transport.tcp;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

final class IncomingMessageChannelInfo extends ChannelInfo {

    public IncomingMessageChannelInfo(SocketChannel channel, StreamIoBuffers buffers, SelectionKey selectionKey) {
        super(channel, buffers, selectionKey);
    }
    
}
