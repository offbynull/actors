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
package com.offbynull.peernetic.rpc.transport.internal;

import com.offbynull.peernetic.common.nio.utils.ByteBufferUtils;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Outgoing response.
 * @param <A> address type
 * @author Kasra Faghihi
 */
public final class SendResponseCommand<A> {
    private ByteBuffer data;

    /**
     * Constructs a {@link SendResponseCommand}.
     * @param data message data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SendResponseCommand(ByteBuffer data) {
        Validate.notNull(data);

        this.data = ByteBufferUtils.copyContents(data);
    }

    /**
     * Constructs a {@link SendResponseCommand}.
     * @param data message data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SendResponseCommand(byte[] data) {
        Validate.notNull(data);

        this.data = ByteBuffer.allocate(data.length).put(data);
        this.data.flip();
    }

    /**
     * Gets a read-only view of the message data.
     * @return message data
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }
}
