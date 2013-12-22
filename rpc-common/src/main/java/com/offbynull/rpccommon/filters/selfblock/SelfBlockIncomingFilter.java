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
package com.offbynull.rpccommon.filters.selfblock;

import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * An {@link IncomingFilter} that checks the first 16 bytes of data to ensure that you aren't sending a message to yourself.
 * @author Kasra F
 * @param <A> address type
 */
public final class SelfBlockIncomingFilter<A> implements IncomingFilter<A> {
    private SelfBlockId id;

    /**
     * Constructs a {@link SelfBlockIncomingFilter}.
     * @param id self block id
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SelfBlockIncomingFilter(SelfBlockId id) {
        Validate.notNull(id);
        
        this.id = id;
    }

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        ByteBuffer incomingIdBuffer = buffer.asReadOnlyBuffer();
        incomingIdBuffer.limit(SelfBlockId.LENGTH);
        
        if (id.getBuffer().equals(incomingIdBuffer)) {
            throw new RuntimeException("Incoming message to self detected");
        }
        
        buffer.position(buffer.position() + SelfBlockId.LENGTH); // adjusted buffer position
        return buffer;
    }
    
}
