package com.offbynull.peernetic.nettyp2p.handlers.common;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.net.SocketAddress;
import java.util.List;

public abstract class AbstractOutgoingTransformHandler extends MessageToMessageEncoder<Object> {

    @Override
    protected final void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        Object res;
        if (msg instanceof AddressedEnvelope) {
            AddressedEnvelope<? extends Object, ? extends SocketAddress> envelopeMsg =
                    (AddressedEnvelope<? extends Object, ? extends SocketAddress>) msg;
            
            Object encoded = transform(envelopeMsg.content());
            
            res = new DefaultAddressedEnvelope<>(encoded, envelopeMsg.recipient(), envelopeMsg.sender());
        } else {
            res = transform(msg);
        }
        
        out.add(res);
    }
    
    protected abstract Object transform(Object obj);
}
