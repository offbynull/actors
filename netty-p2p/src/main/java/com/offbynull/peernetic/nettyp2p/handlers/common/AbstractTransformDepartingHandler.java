package com.offbynull.peernetic.nettyp2p.handlers.common;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.net.SocketAddress;
import java.util.List;

public abstract class AbstractTransformDepartingHandler extends MessageToMessageEncoder<Object> {

    @Override
    protected final void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        Object res;
        if (msg instanceof AddressedEnvelope) {
            AddressedEnvelope<? extends Object, ? extends SocketAddress> envelopeMsg =
                    (AddressedEnvelope<? extends Object, ? extends SocketAddress>) msg;
            
            SocketAddress sender = envelopeMsg.recipient() == null ? ctx.channel().localAddress() : envelopeMsg.recipient();
            SocketAddress recipient = envelopeMsg.sender() == null ? ctx.channel().remoteAddress() : envelopeMsg.sender();
            
            Object encoded = transform(sender, recipient, envelopeMsg.content());
            
            res = new DefaultAddressedEnvelope<>(encoded, envelopeMsg.recipient(), envelopeMsg.sender());
        } else {
            res = transform(ctx.channel().localAddress(), ctx.channel().remoteAddress(), msg);
        }
        
        out.add(res);
    }
    
    protected abstract Object transform(SocketAddress localAddress, SocketAddress remoteAddress, Object obj);
}
