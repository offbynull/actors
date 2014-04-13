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
package com.offbynull.peernetic.nettyextensions.handlers.selfblock;

import com.offbynull.peernetic.nettyextensions.handlers.common.AbstractFilterHandler;
import io.netty.buffer.ByteBuf;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * A Netty handler that checks to see if a {@link SelfBlockId} has been prepended to a {@link ByteBuf}.
 *
 * @author Kasra Faghihi
 */
public final class SelfBlockIdCheckHandler extends AbstractFilterHandler {

    private SelfBlockId id;

    /**
     * Constructs a {@link SelfBlockIdCheckHandler}.
     *
     * @param id self block id
     * @param closeChannelOnFailure close channel if filter throws an exception
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SelfBlockIdCheckHandler(SelfBlockId id, boolean closeChannelOnFailure) {
        super(closeChannelOnFailure);

        Validate.notNull(id);
        this.id = id;
    }

    @Override
    protected boolean filter(SocketAddress local, SocketAddress remote, Object content, Trigger trigger) throws Exception {
        switch (trigger) {
            case READ:
                ByteBuf incomingBuf = (ByteBuf) content;
                ByteBuf incomingIdBuf = incomingBuf.slice(0, SelfBlockId.LENGTH);
                
                ByteBuffer incomingIdByteBuf = ByteBuffer.allocate(SelfBlockId.LENGTH);
                incomingIdBuf.readBytes(incomingIdByteBuf);
                
                incomingBuf.readerIndex(incomingBuf.readerIndex() + SelfBlockId.LENGTH); // move up read ptr by 16
                incomingIdByteBuf.flip();

                return id.getBuffer().equals(incomingIdByteBuf); // if it equals, we don't want this to propogate forward
            default:
                return false;
        }
    }

}
