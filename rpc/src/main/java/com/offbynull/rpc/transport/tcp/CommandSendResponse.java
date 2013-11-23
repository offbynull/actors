package com.offbynull.rpc.transport.tcp;

import com.offbynull.rpc.transport.OutgoingResponse;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

final class CommandSendResponse implements Command {
    private SocketChannel channel;
    private OutgoingResponse response;

    CommandSendResponse(SocketChannel channel, OutgoingResponse response) {
        Validate.notNull(channel);
        Validate.notNull(response);
        
        this.channel = channel;
        this.response = response;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public OutgoingResponse getData() {
        return response;
    }
    
}
