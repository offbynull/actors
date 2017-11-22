/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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

import java.io.Closeable;
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
public final class Bus implements Closeable {

    // Why use this over LinkedBlockingQueue?
    // 1. This has a close() method.
    // 2. This optimizes the locking when adding multiple objects such that a lock is only acquired once. LinkedBlockingQueue.addAll() will
    // acquire and release the lock for each object that's added.
    // 3. This optimizes the locking when reading multiple objects such that a lock is only acquired once. LinkedBlockingQueue requires a
    // call to poll() to know when there's something in the queue and then immediately another call to drainTo() to get the rest of the
    // items in the queue, if any.
    
    private static final Logger LOG = LoggerFactory.getLogger(Bus.class);

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    
    private LinkedList<Object> queue = new LinkedList<>();
    private boolean closed;

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Equivalent to calling {@code add(Collections.singleton(message))}.
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
                condition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Equivalent to calling {@code pull(-1, 0L, TimeUnit.NANOSECONDS)}.
     * @return a list of objects on the bus, or an empty list if the bus was closed
     * @throws InterruptedException if thread is interrupted
     */
    public List<Object> pull() throws InterruptedException {
        return pull(-1, 0L, TimeUnit.NANOSECONDS);
    }

    /**
     * Equivalent to calling {@code pull(-1, timeout, unit)}.
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return a list of objects on the bus, or an empty list if the bus was closed or timed out
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout < 0}
     * @throws InterruptedException if thread is interrupted
     */
    public List<Object> pull(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.isTrue(timeout >= 0L);
        Validate.notNull(unit);
        return pull(-1, timeout, unit);
    }

    /**
     * Reads all messages on this bus, blocking for the specified amount of time until a message becomes available. If no message becomes
     * available in the specified time, this method returns an empty list.
     * @param max maximum number of items to pull (or {code -1} for no limit)
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return a list of objects on the bus, or an empty list if the bus was closed or timed out
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code timeout < 0} or {@code max != -1 && max < 0}
     * @throws InterruptedException if thread is interrupted
     */
    public List<Object> pull(int max, long timeout, TimeUnit unit) throws InterruptedException {
        Validate.isTrue(max == -1 || max >= 0);
        Validate.isTrue(timeout >= 0L);
        Validate.notNull(unit);

        lock.lock();
        try {
            if (closed) {
                LOG.debug("Messages cannot be pulled from a closed bus");
                return new LinkedList<>();
            }

            while (queue.isEmpty()) {
                boolean conditionTriggered;
                if (timeout == 0L) {
                    condition.await();
                    conditionTriggered = true;
                } else {
                    conditionTriggered = condition.await(timeout, unit);
                }

                if (closed) {
                    LOG.debug("Bus was closed while waiting for messages");
                    return new LinkedList<>();
                }

                if (!conditionTriggered) {
                    // timeout elapsed, return without doing anything
                    return new LinkedList<>();
                }
            }
            
            LinkedList<Object> messages;
            if (max == -1 || queue.size() < max) {
                messages = queue;
                queue = new LinkedList<>();
            } else {
                messages = new LinkedList<>();
                for (int i = 0; i < max; i++) {
                    Object message = queue.removeFirst();
                    messages.add(message);
                }
            }
            
            LOG.debug("Pulled {} messages", messages.size());
            return messages;
        } finally {
            lock.unlock();
        }
    }
}
