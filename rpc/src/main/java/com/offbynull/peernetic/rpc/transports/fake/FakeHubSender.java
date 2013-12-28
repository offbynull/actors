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
package com.offbynull.peernetic.rpc.transports.fake;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class FakeHubSender<A> {
    private LinkedBlockingQueue<Command> commandQueue;
    private Line<A> line;

    FakeHubSender(LinkedBlockingQueue<Command> commandQueue, Line<A> line) {
        Validate.notNull(commandQueue);
        Validate.notNull(line);
        this.commandQueue = commandQueue;
        this.line = line;
    }


    public void send(A from, A to, ByteBuffer data) {
        Validate.notNull(from);
        Validate.notNull(to);
        Validate.notNull(data);

        List<Message<A>> messages = line.depart(from, to, data);
        
        List<CommandSend<A>> commandList = new ArrayList<>(messages.size());
        for (Message<A> message : messages) { 
            CommandSend<A> command = new CommandSend<>(message);
            commandList.add(command);
        }
        
        commandQueue.addAll(commandList);
    }
}
