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

import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.peernetic.rpc.transport.OutgoingResponse;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class TcpIncomingMessageResponseHandler implements IncomingMessageResponseHandler {
    private LinkedBlockingQueue<Command> commandQueue;
    private Selector selector;
    private SocketChannel channel;

    TcpIncomingMessageResponseHandler(LinkedBlockingQueue<Command> commandQueue, Selector selector, SocketChannel channel) {
        Validate.notNull(commandQueue);
        Validate.notNull(selector);
        Validate.notNull(channel);
        
        this.commandQueue = commandQueue;
        this.selector = selector;
        this.channel = channel;
    }

    @Override
    public void responseReady(OutgoingResponse response) {
        Validate.notNull(response);
        
        commandQueue.add(new CommandSendResponse(channel, response));
        selector.wakeup();
    }

    @Override
    public void terminate() {
        commandQueue.add(new CommandKillEstablished(channel));
        selector.wakeup();
    }
}
