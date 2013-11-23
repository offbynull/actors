package com.offbynull.rpc.transport.tcp;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

abstract class ChannelInfo {

    private SocketChannel channel;
    private StreamIoBuffers buffers;
    private SelectionKey selectionKey;

    public ChannelInfo(SocketChannel channel, StreamIoBuffers buffers, SelectionKey selectionKey) {
        Validate.notNull(channel);
        Validate.notNull(buffers);
        Validate.notNull(selectionKey);

        this.channel = channel;
        this.buffers = buffers;
        this.selectionKey = selectionKey;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public StreamIoBuffers getBuffers() {
        return buffers;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

}
