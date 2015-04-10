package com.offbynull.peernetic.core.gateways.recorder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WriteBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteBus.class);
    
    private LinkedBlockingQueue<MessageBlock> queue = new LinkedBlockingQueue<>();
    private AtomicBoolean closed = new AtomicBoolean();
    
    void close() {
        closed.set(true);
    }
    
    boolean isClosed() {
        return closed.get();
    }
    
    void add(MessageBlock messageBlock) {
        if (closed.get()) {
            LOGGER.debug("Messages incoming to closed bus: {}", messageBlock);
            return;
        }
        queue.add(messageBlock); // automatically throws NPE if messages contains null, or if messages itself is null
    }
    
    MessageBlock pull() throws InterruptedException {
        return queue.take();
    }
}
