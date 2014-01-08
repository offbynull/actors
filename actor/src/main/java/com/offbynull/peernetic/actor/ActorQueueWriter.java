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
package com.offbynull.peernetic.actor;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.apache.commons.lang3.Validate;

/**
 * Writes messages on to the owning {@link ActorQueue}.
 * @author Kasra Faghihi
 */
final class ActorQueueWriter {
    private final Lock lock;
    private final Condition condition;
    
    // guarded by lock
    private boolean closed;
    private LinkedList<Collection<Incoming>> queue;

    // notifiers need to be thread safe by default
    private final ActorQueueNotifier notifier;

    ActorQueueWriter(LinkedList<Collection<Incoming>> queue, Lock lock, ActorQueueNotifier notifier) {
        Validate.notNull(queue);
        Validate.notNull(lock);
        Validate.notNull(notifier);

        this.queue = queue;
        this.lock = lock;
        this.condition = null;
        this.notifier = notifier;
    }

    ActorQueueWriter(LinkedList<Collection<Incoming>> queue, Lock lock, Condition condition) {
        Validate.notNull(queue);
        Validate.notNull(lock);
        Validate.notNull(condition);

        this.queue = queue;
        this.lock = lock;
        this.condition = condition;
        this.notifier = null;
    }

    /**
     * Pushes messages to the owning {@link ActorQueue} and notifies the {@link Reader} that messages have become available.
     * @param messages from the owning {@link ActorQueue}
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void push(Collection<Incoming> messages) {
        Validate.noNullElements(messages);
    
        if (notifier == null) {
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
        } else {
            lock.lock();
            try {
                if (closed) {
                    return;
                }
                queue.add(new ArrayList<>(messages));
            } finally {
                lock.unlock();
            }
            
            notifier.wakeup();
        }
    }

    /**
     * Pushes messages to the owning {@link ActorQueue} and notifies the {@link Reader} that messages have become available.
     * @param messages from the owning {@link ActorQueue}
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void push(Incoming ... messages) {
        push(Arrays.asList(messages));
    }
    
    void close() {
        if (notifier == null) {
            lock.lock();
            try {
                closed = true;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        } else {
            lock.lock();
            try {
                closed = true;
            } finally {
                lock.unlock();
            }
            
            notifier.wakeup();
        }
    }
}
