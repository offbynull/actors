/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.network.handlers.selfblock;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * An ID used to detect when you're communicating with yourself.
 * @author Kasra Faghihi
 */
public final class SelfBlockId {
    /**
     * Number of bytes required.
     */
    public static final int LENGTH = 16;

    private ByteBuffer id;

    /**
     * Construct a {@link SelfBlockId} using the default {@link SecureRandom} implementation.
     */
    public SelfBlockId() {
        SecureRandom random = new SecureRandom();
        byte[] data = new byte[LENGTH];
        random.nextBytes(data);
        id = ByteBuffer.wrap(data);
    }

    /**
     * Construct a {@link SelfBlockId} using a predetermined bytes.
     * @param id predetermined bytes
     * @throws NullPointerException if any arguments are {@code null} or if {@code id} isn't {@link #LENGTH} bytes long
     */
    public SelfBlockId(ByteBuffer id) {
        Validate.notNull(id);
        Validate.isTrue(id.remaining() == LENGTH);
        
        this.id = ByteBuffer.allocate(id.remaining());
        this.id.put(id);
    }

    /**
     * Get the bytes making up this id.
     * @return bytes making up this id
     */
    public ByteBuffer getBuffer() {
        return id.asReadOnlyBuffer();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.id);
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
        final SelfBlockId other = (SelfBlockId) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
