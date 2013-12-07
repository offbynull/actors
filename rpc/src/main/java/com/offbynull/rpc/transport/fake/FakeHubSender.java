package com.offbynull.rpc.transport.fake;

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
