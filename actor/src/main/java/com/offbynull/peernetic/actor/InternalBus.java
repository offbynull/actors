package com.offbynull.peernetic.actor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalBus.class);
    
    private LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private AtomicBoolean closed = new AtomicBoolean();
    
    void close() {
        closed.set(true);
    }
    
    void add(Collection<Message> messages) {
        if (closed.get()) {
            LOGGER.debug("Messages incoming to closed bus: {}", messages);
            return;
        }
        queue.addAll(messages); // automatically throws NPE if messages contains null, or if messages itself is null
    }
    
    List<Message> pull() throws InterruptedException {
        List<Message> messages = new LinkedList<>();
        
        Message first = queue.take();
        messages.add(first);
        queue.drainTo(messages);
        
        return messages;
    }
}
