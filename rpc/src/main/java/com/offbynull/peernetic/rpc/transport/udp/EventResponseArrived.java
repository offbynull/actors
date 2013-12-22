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
package com.offbynull.peernetic.rpc.transport.udp;

import com.offbynull.peernetic.rpc.transport.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class EventResponseArrived implements Event {
    private IncomingResponse<InetSocketAddress> response;
    private OutgoingMessageResponseListener<InetSocketAddress> receiver;

    EventResponseArrived(IncomingResponse<InetSocketAddress> response, OutgoingMessageResponseListener<InetSocketAddress> receiver) {
        Validate.notNull(response);
        Validate.notNull(receiver);
        
        this.response = response;
        this.receiver = receiver;
    }

    public IncomingResponse<InetSocketAddress> getResponse() {
        return response;
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getReceiver() {
        return receiver;
    }
    
    
}
