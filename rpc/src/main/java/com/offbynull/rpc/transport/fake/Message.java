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
package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Transit message.
 * @author Kasra F
 * @param <A> address type
 */
public final class Message<A> {
    private A from;
    private A to;
    private ByteBuffer data;
    private long arriveTime;

    /**
     * Constructs a {@link Message} object.
     * @param from source
     * @param to destination
     * @param data contents
     * @param arriveTime time the packet should arrive at its destination
     * @throws NullPointerException if any arguments are {@code null}
     */
    public Message(A from, A to, ByteBuffer data, long arriveTime) {
        Validate.notNull(from);
        Validate.notNull(to);
        Validate.notNull(data);
        this.from = from;
        this.to = to;
        this.data = ByteBuffer.allocate(data.remaining());
        this.data.put(data);
        this.data.flip();
        this.arriveTime = arriveTime;
    }

    /**
     * Get source address.
     * @return source
     */
    public A getFrom() {
        return from;
    }

    /**
     * Get destination address.
     * @return destination
     */
    public A getTo() {
        return to;
    }

    /**
     * Get contents as read-only buffer.
     * @return contents
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }

    /**
     * Get time the message should arrive at its destination.
     * @return time the message should arrive
     */
    public long getArriveTime() {
        return arriveTime;
    }
    
}
