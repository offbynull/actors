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
 * Outgoing message.
 * @author Kasra F
 * @param <A> address type
 */
public final class OutgoingMessage<A> {
    private A to;
    private ByteBuffer data;

    /**
     * Constructs a {@link OutgoingMessage}.
     * @param to destination address
     * @param data message data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public OutgoingMessage(A to, ByteBuffer data) {
        Validate.notNull(to);
        Validate.notNull(data);
        
        this.to = to;
        this.data = ByteBuffer.allocate(data.remaining()).put(data);
        this.data.flip();
    }

    /**
     * Constructs a {@link OutgoingMessage}.
     * @param to destination address
     * @param data message data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public OutgoingMessage(A to, byte[] data) {
        Validate.notNull(to);
        Validate.notNull(data);
        
        this.to = to;
        this.data = ByteBuffer.allocate(data.length).put(data);
        this.data.flip();
    }

    /**
     * Get destination address.
     * @return destination address
     */
    public A getTo() {
        return to;
    }

    /**
     * Gets a read-only view of the message data.
     * @return message data
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }
    
}
