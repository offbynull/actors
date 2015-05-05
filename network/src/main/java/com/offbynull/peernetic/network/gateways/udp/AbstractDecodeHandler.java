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

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.SocketAddress;
import java.util.List;

abstract class AbstractDecodeHandler extends MessageToMessageDecoder<Object> {

    @Override
    protected final void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        Object res;
        if (msg instanceof AddressedEnvelope) {
            AddressedEnvelope<ByteBuf, ? extends SocketAddress> envelopeMsg =
                    (AddressedEnvelope<ByteBuf, ? extends SocketAddress>) msg;
            
            Object decoded = decode(envelopeMsg.content());
            
            res = new DefaultAddressedEnvelope<>(decoded, envelopeMsg.recipient(), envelopeMsg.sender());
        } else {
            res = decode((ByteBuf) msg);
        }
        
        out.add(res);
    }
    
    protected abstract Object decode(ByteBuf buf);
    
}
