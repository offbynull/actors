package com.offbynull.peernetic.core.gateways.udp;

import com.offbynull.peernetic.core.Message;
import com.offbynull.peernetic.core.Shuttle;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

final class IncomingMessageShuttleHandler extends MessageToMessageDecoder<Object> {
    private String srcAddressPrefix;
    private Shuttle dstShuttle;
    private String dstAddress;

    IncomingMessageShuttleHandler(String srcAddressPrefix, Shuttle dstShuttle, String dstAddress) {
        Validate.notNull(srcAddressPrefix);
        Validate.notNull(dstShuttle);
        Validate.notNull(dstAddress);
        Validate.notEmpty(srcAddressPrefix);
        Validate.notEmpty(dstAddress);

        this.srcAddressPrefix = srcAddressPrefix;
        this.dstShuttle = dstShuttle;
        this.dstAddress = dstAddress;
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
        EncapsulatedMessage em = (EncapsulatedMessage) obj;
        String srcAddress = srcAddressPrefix + SEPARATOR + toShuttleAddress(remoteAddress);
        String srcSuffix = em.getAddressSuffix();
        if (srcSuffix != null) {
            srcAddress += SEPARATOR + em.getAddressSuffix();
        }
        
        Object msgObj = em.getObject();
        
        Message message = new Message(
                srcAddress,
                dstAddress,
                msgObj);
        dstShuttle.send(Collections.singleton(message));
        
        return obj;
    }
    
    private static String toShuttleAddress(SocketAddress address) {
        byte[] addr = ((InetSocketAddress) address).getAddress().getAddress();
        int port = ((InetSocketAddress) address).getPort();
        
        return Hex.encodeHexString(addr) + '.' + port;
    }
}
