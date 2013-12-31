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
package com.offbynull.peernetic.common.concurrent.pumps.queueandselector;

import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.lang3.Validate;

class QueueAndSelectorPumpReader<T> implements PumpReader<T> {
    private LinkedBlockingQueue<Iterator<T>> queue;
    private Selector selector;

    QueueAndSelectorPumpReader(LinkedBlockingQueue<Iterator<T>> queue, Selector selector) {
        Validate.notNull(queue);
        Validate.notNull(selector);

        this.queue = queue;
        this.selector = selector;
    }

    @Override
    public Iterator<T> pull(long timeout) throws InterruptedException, IOException {
        Validate.validState(selector.isOpen());
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);

        if (timeout == 0L) {
            selector.selectNow();
        } else {
            selector.select(timeout);
        }
        
        LinkedList<Iterator<T>> dst = new LinkedList<>();
        queue.drainTo(dst);
        
        IteratorChain<T> chain = new IteratorChain();
        for (Iterator<T> batch : dst) {
            chain.addIterator(batch);
        }
        
        return IteratorUtils.unmodifiableIterator(chain);
    }
    
}
