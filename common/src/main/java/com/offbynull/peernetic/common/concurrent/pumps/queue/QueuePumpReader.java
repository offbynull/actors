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

import com.offbynull.peernetic.common.concurrent.pump.Message;
import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.lang3.Validate;

final class QueuePumpReader implements PumpReader {
    private LinkedBlockingQueue<Iterator<Message>> queue;

    QueuePumpReader(LinkedBlockingQueue<Iterator<Message>> queue) {
        Validate.notNull(queue);

        this.queue = queue;
    }

    @Override
    public Iterator<Message> pull(long timeout) throws InterruptedException {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);
        
        LinkedList<Iterator> dst = new LinkedList<>();
        Iterator first = queue.poll(timeout, TimeUnit.MILLISECONDS);
        
        if (first == null) {
            return IteratorUtils.emptyIterator();
        }

        dst.add(first);
        queue.drainTo(dst);
        
        IteratorChain chain = new IteratorChain();
        for (Iterator batch : dst) {
            chain.addIterator(batch);
        }
        
        return IteratorUtils.unmodifiableIterator(chain);
    }
    
}
