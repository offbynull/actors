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
package com.offbynull.peernetic.rpc.transports.tcp;

import com.offbynull.peernetic.rpc.transport.IncomingMessage;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

final class EventRequestArrived implements Event {
    private IncomingMessage<InetSocketAddress> request;
    private Selector selector;
    private SocketChannel channel;

    EventRequestArrived(IncomingMessage<InetSocketAddress> request, Selector selector, SocketChannel channel) {
        Validate.notNull(request);
        Validate.notNull(selector);
        Validate.notNull(channel);
        
        this.request = request;
        this.selector = selector;
        this.channel = channel;
    }

    public IncomingMessage<InetSocketAddress> getRequest() {
        return request;
    }

    public Selector getSelector() {
        return selector;
    }

    public SocketChannel getChannel() {
        return channel;
    }
    
}
