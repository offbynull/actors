package com.offbynull.peernetic.core.shuttles.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bus implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bus.class);

    private LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private AtomicBoolean closed = new AtomicBoolean();

    @Override
    public void close() {
        closed.set(true);
    }

    public void add(Object message) {
        add(Collections.singleton(message));
    }
    
    public void add(Collection<?> messages) {
        if (closed.get()) {
            LOGGER.debug("Messages incoming to closed bus: {}", messages);
            return;
        }
        queue.addAll(messages); // automatically throws NPE if messages contains null, or if messages itself is null
    }

    public List<Object> pull(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.isTrue(timeout >= 0L);
        Validate.notNull(unit);
        List<Object> messages = new LinkedList<>();

        Object first = queue.poll(timeout, unit);
        if (first != null) { // if it didn't time out, 
            messages.add(first);
            queue.drainTo(messages);
        }

        return messages;
    }

    public List<Object> pull() throws InterruptedException {
        List<Object> messages = new LinkedList<>();

        Object first = queue.take();
        messages.add(first);
        queue.drainTo(messages);

        return messages;
    }
}
