/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc.transports.udp;

import com.offbynull.peernetic.rpc.transport.OutgoingResponse;
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
