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
package com.offbynull.actors.core.shuttles.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Bus} allows reading and writing of objects in a thread-safe manner. You can {@link #close() } a bus such that it no longer
 * accepts incoming messages.
 * @author Kasra Faghihi
 */
public final class Bus implements AutoCloseable {

    // Why use this over LinkedBlockingQueue?
    // 1. This has a close() method.
    // 2. This optimizes the locking when adding multiple objects such that a lock is only acquired once. LinkedBlockingQueue.addAll() will
    // acquire and release the lock for each object that's added.
    // 3. This optimizes the locking when reading multiple objects such that a lock is only acquired once. LinkedBlockingQueue requires a
    // call to poll() to know when there's something in the queue and then immediately another call to drainTo() to get the rest of the
    // items in the queue, if any.
    
    private static final Logger LOG = LoggerFactory.getLogger(Bus.class);

    private final Lock lock = new ReentrantLock();
    private final Condition newMessagesCondition = lock.newCondition();
    
    private LinkedList<Object> queue = new LinkedList<>();
    private boolean closed;

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;
        } finally {
            lock.unlock();
        }
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
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        lock.lock();
        try {
            if (closed) {
                LOG.debug("Messages incoming to closed bus: {}", messages);
                return;
            }
            queue.addAll(messages);
            
            if (!queue.isEmpty()) {
                newMessagesCondition.signal();
            }
        } finally {
            lock.unlock();
        }
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
        
        lock.lock();
        try {
            while (queue.isEmpty()) {
                boolean conditionTriggered = newMessagesCondition.await(timeout, unit);
                if (!conditionTriggered) {
                    // timeout elapsed, return without doing anything
                    return new LinkedList<>();
                }
            }
            
            List<Object> messages = queue;
            queue = new LinkedList<>();
            
            LOG.debug("Pulled {} messages", messages.size());
            return messages;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reads a message from this bus, blocking indefinitely until a message becomes available.
     * @return a list of objects on the bus
     * @throws InterruptedException if thread is interrupted
     */
    public List<Object> pull() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                newMessagesCondition.await();
            }
            
            List<Object> messages = queue;
            queue = new LinkedList<>();
            
            LOG.debug("Pulled {} messages", messages.size());
            return messages;
        } finally {
            lock.unlock();
        }
    }
}
