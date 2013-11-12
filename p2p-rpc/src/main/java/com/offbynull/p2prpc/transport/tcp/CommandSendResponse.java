package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.OutgoingData;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

final class CommandSendResponse implements Command {
    private SocketChannel channel;
    private OutgoingData<InetSocketAddress> data;

    CommandSendResponse(SocketChannel channel, OutgoingData<InetSocketAddress> data) {
        Validate.notNull(channel);
        Validate.notNull(data);
        
        this.channel = channel;
        this.data = data;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public OutgoingData<InetSocketAddress> getData() {
        return data;
    }
    
}
