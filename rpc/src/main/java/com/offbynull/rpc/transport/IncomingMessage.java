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
package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Incoming message.
 * @author Kasra F
 * @param <A> address type
 */
public final class IncomingMessage<A> {
    private A from;
    private ByteBuffer data;
    private long arriveTime;

    /**
     * Constructs an {@link IncomingMessage} object.
     * @param from source address
     * @param data message data
     * @param arriveTime arrival time
     * @throws NullPointerException if any arguments are {@code null}
     */
    public IncomingMessage(A from, ByteBuffer data, long arriveTime) {
        Validate.notNull(from);
        Validate.notNull(data);
        
        this.from = from;
        this.data = ByteBuffer.allocate(data.remaining()).put(data);
        this.arriveTime = arriveTime;
        this.data.flip();
    }

    /**
     * Constructs an {@link IncomingMessage} object.
     * @param from source address
     * @param data message data
     * @param arriveTime arrival time
     * @throws NullPointerException if any arguments are {@code null}
     */
    public IncomingMessage(A from, byte[] data, long arriveTime) {
        Validate.notNull(from);
        Validate.notNull(data);
        
        this.from = from;
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.arriveTime = arriveTime;
        this.data.flip();
    }

    /**
     * Get source address.
     * @return source address
     */
    public A getFrom() {
        return from;
    }

    /**
     * Gets a read-only view of the message data.
     * @return message data
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }

    /**
     * Gets the arrival time.
     * @return arrival time
     */
    public long getArriveTime() {
        return arriveTime;
    }
    
}
