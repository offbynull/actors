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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An {@link Endpoint} implementation that queues up incoming messages internally and dumps them out when 
 * {@link #poll(long, java.util.concurrent.TimeUnit) } is invoked.
 * @author Kasra Faghihi
 */
public final class TemporaryEndpoint implements Endpoint {
    private LinkedBlockingQueue<Object> incoming = new LinkedBlockingQueue<>(1);

    @Override
    public void push(Endpoint source, Collection<Outgoing> outgoing) {
        for (Outgoing outgoingMsg : outgoing) {
            incoming.add(outgoingMsg.getContent());
        }
    }

    @Override
    public void push(Endpoint source, Outgoing... outgoing) {
        push(source, Arrays.asList(outgoing));
    }

    /**
     * Dump out incoming messages sitting in the queue.
     * @param timeout timeout duration
     * @param unit timeout units
     * @return queued incoming messages
     * @throws InterruptedException if thread is interrupted
     */
    public List<Object> poll(long timeout, TimeUnit unit) throws InterruptedException {
        List<Object> ret = new ArrayList<>();
        Object obj = incoming.poll(timeout, unit);
        if (obj != null) {
            ret.add(obj);
            incoming.drainTo(ret);
        }
        
        return ret;
    }
}
