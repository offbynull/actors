package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

final class OutgoingMessageChannelInfo extends ChannelInfo {

    private OutgoingMessageResponseListener<InetSocketAddress> responseHandler;

    public OutgoingMessageChannelInfo(SocketChannel channel, StreamIoBuffers buffers, SelectionKey selectionKey,
            OutgoingMessageResponseListener<InetSocketAddress> responseListener) {
        super(channel, buffers, selectionKey);

        Validate.notNull(responseListener); // may be null

        this.responseHandler = responseListener;
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getResponseHandler() {
        return responseHandler;
    }
    
}
