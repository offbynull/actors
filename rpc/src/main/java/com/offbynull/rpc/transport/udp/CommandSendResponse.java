package com.offbynull.rpc.transport.udp;

import com.offbynull.rpc.transport.OutgoingResponse;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class CommandSendResponse implements Command {
    private MessageId messageId;
    private InetSocketAddress address;
    private OutgoingResponse response;

    CommandSendResponse(MessageId messageId, InetSocketAddress address, OutgoingResponse response) {
        Validate.notNull(messageId);
        Validate.notNull(address);
        Validate.notNull(response);
        
        this.messageId = messageId;
        this.address = address;
        this.response = response;
    }

    public MessageId getMessageId() {
        return messageId;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public OutgoingResponse getResponse() {
        return response;
    }
    
}
