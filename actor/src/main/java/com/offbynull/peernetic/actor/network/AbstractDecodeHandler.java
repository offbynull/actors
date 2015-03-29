package com.offbynull.peernetic.actor.network;

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
