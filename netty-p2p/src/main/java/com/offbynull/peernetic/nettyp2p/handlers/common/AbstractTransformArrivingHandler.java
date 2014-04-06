package com.offbynull.peernetic.nettyp2p.handlers.common;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.SocketAddress;
import java.util.List;

public abstract class AbstractTransformArrivingHandler extends MessageToMessageDecoder<Object> {

    @Override
    protected final void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        Object res;
        if (msg instanceof AddressedEnvelope) {
            AddressedEnvelope<? extends Object, ? extends SocketAddress> envelopeMsg =
                    (AddressedEnvelope<? extends Object, ? extends SocketAddress>) msg;
            
            SocketAddress sender = envelopeMsg.sender() == null ? ctx.channel().remoteAddress() : envelopeMsg.sender();
            SocketAddress recipient = envelopeMsg.recipient() == null ? ctx.channel().localAddress() : envelopeMsg.recipient();
            
            Object encoded = transform(recipient, sender, envelopeMsg.content());
            
            res = new DefaultAddressedEnvelope<>(encoded, envelopeMsg.recipient(), envelopeMsg.sender());
        } else {
            res = transform(ctx.channel().localAddress(), ctx.channel().remoteAddress(), msg);
        }
        
        out.add(res);
    }
    
    protected abstract Object transform(SocketAddress localAddress, SocketAddress remoteAddress, Object obj);
}
