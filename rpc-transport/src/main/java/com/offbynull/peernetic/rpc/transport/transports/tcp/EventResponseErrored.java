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
package com.offbynull.peernetic.rpc.transport.transports.tcp;

import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

final class EventResponseErrored implements Event {
    private Throwable error;
    private OutgoingMessageResponseListener<InetSocketAddress> receiver;

    EventResponseErrored(Throwable error, OutgoingMessageResponseListener<InetSocketAddress> receiver) {
        Validate.notNull(error);
        Validate.notNull(receiver);
        
        this.error = error;
        this.receiver = receiver;
    }

    public Throwable getError() {
        return error;
    }

    public OutgoingMessageResponseListener<InetSocketAddress> getReceiver() {
        return receiver;
    }
    
    
}
