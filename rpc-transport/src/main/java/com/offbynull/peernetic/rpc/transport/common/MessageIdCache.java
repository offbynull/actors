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
package com.offbynull.peernetic.rpc.transport.common;

import java.util.HashSet;
import java.util.LinkedList;
import org.apache.commons.lang3.Validate;

/**
 * A fixed-size cache of {@link MessageId}s. Used to throw away duplicate messages.
 * @author Kasra Faghihi
 * @param <A> address type
 */
final class MessageIdCache<A> {

    private int capacity;
    private LinkedList<MessageIdInstance> queue;
    private HashSet<MessageIdInstance> set;

    public MessageIdCache(int capacity) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, capacity);

        this.capacity = capacity;
        this.queue = new LinkedList<>();
        this.set = new HashSet<>(capacity);
    }

    public boolean add(A from, MessageId id, MessageType type) {
        Validate.notNull(id);
        Validate.notNull(type);
        
        MessageIdInstance idInstance = new MessageIdInstance(from, id, type);

        if (set.contains(idInstance)) {
            return false;
        }

        if (queue.size() == capacity) {
            MessageIdInstance last = queue.removeLast();
            set.remove(last);
        }

        queue.addFirst(idInstance);
        set.add(idInstance);

        return true;
    }
}
