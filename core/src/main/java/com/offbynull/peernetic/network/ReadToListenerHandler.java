package com.offbynull.peernetic.network;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class ReadToListenerHandler extends MessageToMessageDecoder<Object> {
    private Gateway<InetSocketAddress> gateway;
    private GatewayListener listener;

    public ReadToListenerHandler(Gateway<InetSocketAddress> gateway, GatewayListener listener) {
        Validate.notNull(gateway);
        Validate.notNull(listener);

        this.gateway = gateway;
        this.listener = listener;
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
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
    
    protected Object transform(SocketAddress localAddress, SocketAddress remoteAddress, Object obj) {
        try {
            listener.onReadMessage(new Message((InetSocketAddress) localAddress,
                    (InetSocketAddress) remoteAddress,
                    obj,
                    gateway));
        } catch (Exception e) {
            System.err.println(e);
            // do nothing
        }
        
        return obj;
    }
}
