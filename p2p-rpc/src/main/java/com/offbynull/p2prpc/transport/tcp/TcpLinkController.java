package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.SessionedTransport;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class TcpLinkController implements SessionedTransport.LinkController {
        private Long id;
        private SocketChannel channel;
        private Selector selector;
        private LinkedBlockingQueue<Command> commandQueue;

        public TcpLinkController(long id, Selector selector, LinkedBlockingQueue<Command> commandQueue) {
            Validate.notNull(selector);
            Validate.notNull(commandQueue);
            this.id = id;
            this.selector = selector;
            this.commandQueue = commandQueue;
        }

        public TcpLinkController(SocketChannel channel, Selector selector, LinkedBlockingQueue<Command> commandQueue) {
            Validate.notNull(channel);
            Validate.notNull(selector);
            Validate.notNull(commandQueue);
            this.channel = channel;
            this.selector = selector;
            this.commandQueue = commandQueue;
        }


        @Override
        public void kill() {
            if (id != null) {
                commandQueue.add(new CommandKillQueued(id));
            } else if (channel != null) {
                commandQueue.add(new CommandKillEstablished(channel));
            }
            selector.wakeup();
        }
        
}
