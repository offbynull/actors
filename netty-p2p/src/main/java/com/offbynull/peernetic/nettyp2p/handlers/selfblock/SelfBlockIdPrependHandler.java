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
package com.offbynull.peernetic.nettyp2p.handlers.selfblock;

import com.offbynull.peernetic.nettyp2p.handlers.common.AbstractEncodeHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.Validate;

/**
 * A Netty handler that prepends a {@link SelfBlockId} to the beginning of a {@link ByteBuf}.
 * @author Kasra Faghihi
 */
public final class SelfBlockIdPrependHandler extends AbstractEncodeHandler {
    private SelfBlockId id;

    /**
     * Constructs a {@link SelfBlockOutgoingFilter}.
     * @param id self block id
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SelfBlockIdPrependHandler(SelfBlockId id) {
        Validate.notNull(id);
        
        this.id = id;
    }

    @Override
    protected ByteBuf encode(Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        ByteBuf newBuffer = Unpooled.wrappedBuffer(new byte[SelfBlockId.LENGTH + buf.readableBytes()]);
        buf.readerIndex(0);
        newBuffer.writeBytes(id.getBuffer());
        newBuffer.writeBytes(buf);
        
        buf.release(); // is this required?
        
        return newBuffer;
    }
    
}
