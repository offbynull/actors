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
package com.offbynull.peernetic.common.concurrent.actor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.lang3.Validate;

/**
 * Reads messages from the owning {@link ActorQueue}.
 * @author Kasra Faghihi
 */
public final class ActorQueueReader {
    private boolean closed;
    private LinkedList<Iterator<Outgoing>> queue;
    private Lock lock;
    private Condition condition;
    private ActorQueueNotifier notifier;

    ActorQueueReader(LinkedList<Iterator<Outgoing>> queue, Lock lock, ActorQueueNotifier notifier) {
        Validate.notNull(queue);
        Validate.notNull(lock);
        Validate.notNull(notifier);

        this.queue = queue;
        this.lock = lock;
        this.notifier = notifier;
    }

    ActorQueueReader(LinkedList<Iterator<Outgoing>> queue, Lock lock, Condition condition) {
        Validate.notNull(queue);
        Validate.notNull(lock);
        Validate.notNull(condition);

        this.queue = queue;
        this.lock = lock;
        this.condition = condition;
    }

    /**
     * Pulls messages from the owning {@link ActorQueue} if available, or if empty blocks until messages become available / the underlying
     * {@link ActorQueueNotifier} wakes up (if any).
     * @param timeout maximum amount of time to block
     * @return messages from the owning {@link ActorQueue}
     * @throws InterruptedException if thread is interrupted
     */
    public Iterator<Outgoing> pull(long timeout) throws InterruptedException {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);
        
        LinkedList<Iterator> dst = new LinkedList<>();
        
        if (notifier == null) {
            lock.lock();
            try {
                if (closed) {
                    return IteratorUtils.emptyIterator();
                }

                if (queue.isEmpty()) { 
                    condition.await(timeout, TimeUnit.MILLISECONDS);
                }
                
                if (closed) {
                    return IteratorUtils.emptyIterator();
                }

                dst.addAll(queue);
                queue.clear();
            } finally {
                lock.unlock();
            }
        } else {
            notifier.await(timeout);
            
            lock.lock();
            try {
                if (closed) {
                    return IteratorUtils.emptyIterator();
                }
                
                dst.addAll(queue);
                queue.clear();
            } finally {
                lock.unlock();
            }
        }
        
        IteratorChain chain = new IteratorChain();
        for (Iterator batch : dst) {
            chain.addIterator(batch);
        }
        
        return IteratorUtils.unmodifiableIterator(chain);
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
