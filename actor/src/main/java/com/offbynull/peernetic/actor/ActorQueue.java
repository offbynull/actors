/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * Message pump.
 *
 * @author Kasra Faghihi
 */
final class ActorQueue {

    private final Lock lock;
    private final Condition condition;

    // guarded by lock
    private LinkedList<Collection<Incoming>> queue;
    private boolean closed;

    /**
     * Constructs an {@link ActorQueue} object.
     */
    public ActorQueue() {
        this.queue = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    /**
     * Pulls messages from the owning {@link ActorQueue} if available, or if empty blocks until messages become available / the underlying
     * {@link ActorQueueNotifier} wakes up (if any).
     *
     * @param timeout maximum amount of time to block
     * @return messages from the owning {@link ActorQueue}
     * @throws InterruptedException if thread is interrupted
     */
    public Collection<Incoming> pull(long timeout) throws InterruptedException {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);

        LinkedList<Collection<Incoming>> dst = new LinkedList<>();

        lock.lock();
        try {
            if (closed) {
                return Collections.emptySet();
            }

            if (queue.isEmpty()) {
                condition.await(timeout, TimeUnit.MILLISECONDS);
            }

            if (closed) {
                return Collections.emptySet();
            }

            dst.addAll(queue);
            queue.clear();
        } finally {
            lock.unlock();
        }

        int size = 0;
        for (Collection<Incoming> batch : dst) {
            size += batch.size();
        }

        ArrayList<Incoming> incoming = new ArrayList<>(size);
        for (Collection<Incoming> batch : dst) {
            incoming.addAll(batch);
        }

        return Collections.unmodifiableCollection(incoming);
    }

    /**
     * Pushes messages to the owning {@link ActorQueue} and notifies the {@link Reader} that messages have become available.
     *
     * @param messages from the owning {@link ActorQueue}
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void push(Collection<Incoming> messages) {
        Validate.noNullElements(messages);

        lock.lock();
        try {
            if (closed) {
                return;
            }

            queue.add(new ArrayList<>(messages));
            condition.signal();
        } finally {
            lock.unlock();
        }

    }

    /**
     * Pushes messages to the owning {@link ActorQueue} and notifies the {@link Reader} that messages have become available.
     *
     * @param messages from the owning {@link ActorQueue}
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void push(Incoming... messages) {
        push(Arrays.asList(messages));
    }

    void close() {
        lock.lock();
        try {
            closed = true;
            condition.signalAll();
        } finally {
            lock.unlock();
        }

    }
}
