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
package com.offbynull.peernetic.common.concurrent.pumps.queue;

import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.lang3.Validate;

final class QueuePumpReader<T> implements PumpReader<T> {
    private LinkedBlockingQueue<Iterator<T>> queue;

    QueuePumpReader(LinkedBlockingQueue<Iterator<T>> queue) {
        Validate.notNull(queue);

        this.queue = queue;
    }

    @Override
    public Iterator<T> pull(long timeout) throws InterruptedException {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);
        
        LinkedList<Iterator<T>> dst = new LinkedList<>();
        Iterator<T> first = queue.poll(timeout, TimeUnit.MILLISECONDS);
        
        if (first == null) {
            return IteratorUtils.emptyIterator();
        }

        dst.add(first);
        queue.drainTo(dst);
        
        IteratorChain<T> chain = new IteratorChain();
        for (Iterator<T> batch : dst) {
            chain.addIterator(batch);
        }
        
        return IteratorUtils.unmodifiableIterator(chain);
    }
    
}
