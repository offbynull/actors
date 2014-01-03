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

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

/**
 * Encapsulates a message id.
 * @author Kasra Faghihi
 */
public final class MessageId {
    /**
     * Number of bytes for a message id.
     */
    public static final int MESSAGE_ID_SIZE = 16;
    
    private byte[] id;

    /**
     * Constructs a {@link MessageId} object.
     * @param id message id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@link id} is not 16 bytes long
     */
    public MessageId(byte[] id) {
        Validate.isTrue(id.length == MESSAGE_ID_SIZE);
        
        this.id = Arrays.copyOf(id, id.length);
    }
    
    /**
     * Write in to a {@link ByteBuffer}.
     * @param buffer buffer to write to
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code buffer} has less than 16 bytes remaining
     */
    public void writeId(ByteBuffer buffer) {
        Validate.notNull(buffer);
        Validate.isTrue(buffer.remaining() >= MESSAGE_ID_SIZE);
        
        buffer.put(id);
    }

    /**
     * Read from a {@link ByteBuffer}.
     * @param buffer buffer to read from
     * @return {@link MessageId} read from the buffer
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code buffer} has less than 16 bytes remaining
     */
    public static MessageId readId(ByteBuffer buffer) {
        Validate.notNull(buffer);
        Validate.isTrue(buffer.remaining() >= MESSAGE_ID_SIZE);
        
        byte[] extractedId = new byte[MESSAGE_ID_SIZE];
        buffer.get(extractedId);
        
        return new MessageId(extractedId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MessageId other = (MessageId) obj;
        if (!Arrays.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
