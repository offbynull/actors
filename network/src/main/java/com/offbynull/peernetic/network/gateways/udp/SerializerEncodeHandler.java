/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.network.gateways.udp;

import com.offbynull.peernetic.core.common.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.Validate;


final class SerializerEncodeHandler extends AbstractEncodeHandler {

    private Serializer serializer;

    SerializerEncodeHandler(Serializer serializer) {
        Validate.notNull(serializer);
        this.serializer = serializer;
    }

    @Override
    protected ByteBuf encode(Object obj) {
        Validate.notNull(obj);
        
        byte[] data = serializer.serialize(obj);
        return Unpooled.wrappedBuffer(data);
    }
}
