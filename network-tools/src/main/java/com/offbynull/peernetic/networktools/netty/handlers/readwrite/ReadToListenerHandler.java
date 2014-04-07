package com.offbynull.peernetic.networktools.netty.handlers.readwrite;

import com.offbynull.peernetic.networktools.netty.handlers.common.AbstractTransformArrivingHandler;
import io.netty.channel.ChannelHandlerContext;
import java.net.SocketAddress;
import org.apache.commons.lang3.Validate;

public final class ReadToListenerHandler extends AbstractTransformArrivingHandler {
    private IncomingMessageListener listener;

    public ReadToListenerHandler(IncomingMessageListener listener) {
        Validate.notNull(listener);

        this.listener = listener;
    }
    
    @Override
    protected Object transform(ChannelHandlerContext ctx, SocketAddress localAddress, SocketAddress remoteAddress, Object obj) {
        try {
            listener.newMessage(new Message(localAddress, remoteAddress, obj, ctx.channel()));
        } catch (Exception e) {
            // do nothing
        }
        
        return obj;
    }
    
}
