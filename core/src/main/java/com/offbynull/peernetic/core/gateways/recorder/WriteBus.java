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
package com.offbynull.peernetic.core.gateways.recorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WriteBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteBus.class);
    
    private LinkedBlockingQueue<MessageBlock> queue = new LinkedBlockingQueue<>();
    private AtomicBoolean closed = new AtomicBoolean();
    
    void close() {
        closed.set(true);
    }
    
    boolean isClosed() {
        return closed.get();
    }
    
    void add(MessageBlock messageBlock) {
        if (closed.get()) {
            LOGGER.debug("Messages incoming to closed bus: {}", messageBlock);
            return;
        }
        queue.add(messageBlock); // automatically throws NPE if messages contains null, or if messages itself is null
    }
    
    MessageBlock pull() throws InterruptedException {
        return queue.take();
    }

    List<MessageBlock> drain() {
        List<MessageBlock> ret = new ArrayList<>();
        queue.drainTo(ret);
        return ret;
    }
}
