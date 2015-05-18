/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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

/**
 * A {@link Bus} is similar to a {@link LinkedBlockingQueue} in that it allows you to write and read objects in a thread-safe manner. In
 * addition to that, you can {@link #close() } a bus such that it no longer accepts incoming messages.
 * @author Kasra Faghihi
 */
public final class Bus implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Bus.class);

    private LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private AtomicBoolean closed = new AtomicBoolean();

    @Override
    public void close() {
        closed.set(true);
    }

    /**
     * Put a single message on to this bus. If this bus has been closed, this method does nothing.
     * @param message message
     * @throws NullPointerException if any argument is {@code null}
     */
    public void add(Object message) {
        add(Collections.singleton(message));
    }
    
    /**
     * Adds a collection of messages on to this bus. If this bus has been closed, this method does nothing.
     * @param messages messages to add
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void add(Collection<?> messages) {
        if (closed.get()) {
            LOG.debug("Messages incoming to closed bus: {}", messages);
            return;
        }
        queue.addAll(messages); // automatically throws NPE if messages contains null, or if messages itself is null
    }

    /**
     * Reads a message from this bus, blocking for the specified amount of time until a message becomes available. If no message becomes
     * available in the specified time, this method returns an empty list.
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return a list of objects on the bus
     * @throws InterruptedException if thread is interrupted
     */
    public List<Object> pull(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.isTrue(timeout >= 0L);
        Validate.notNull(unit);
        List<Object> messages = new LinkedList<>();

        Object first = queue.poll(timeout, unit);
        if (first != null) { // if it didn't time out, 
            messages.add(first);
            queue.drainTo(messages);
        }
        
        LOG.debug("Pulled {} messages", messages.size());

        return messages;
    }

    /**
     * Reads a message from this bus, blocking indefinitely until a message becomes available.
     * @return a list of objects on the bus
     * @throws InterruptedException if thread is interrupted
     */
    public List<Object> pull() throws InterruptedException {
        List<Object> messages = new LinkedList<>();

        Object first = queue.take();
        messages.add(first);
        queue.drainTo(messages);

        LOG.debug("Pulled {} messages", messages.size());
        
        return messages;
    }
}
