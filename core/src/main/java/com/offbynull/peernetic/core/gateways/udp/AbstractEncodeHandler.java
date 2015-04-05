package com.offbynull.peernetic.core.gateways.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.net.SocketAddress;
import java.util.List;

abstract class AbstractEncodeHandler extends MessageToMessageEncoder<Object> {

    @Override
    protected final void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        Object res;
        if (msg instanceof AddressedEnvelope) {
            AddressedEnvelope<? extends Object, ? extends SocketAddress> envelopeMsg =
                    (AddressedEnvelope<? extends Object, ? extends SocketAddress>) msg;
            
            ByteBuf encoded = encode(envelopeMsg.content());
            
            res = new DefaultAddressedEnvelope<>(encoded, envelopeMsg.recipient(), envelopeMsg.sender());
        } else {
            res = encode(msg);
        }
        
        out.add(res);
    }
    
    protected abstract ByteBuf encode(Object obj);
}
