package com.offbynull.peernetic.nettyp2p.handlers.read;

import com.offbynull.peernetic.nettyp2p.handlers.common.AbstractTransformArrivingHandler;
import java.net.SocketAddress;
import org.apache.commons.lang3.Validate;

public final class ReadToListenerHandler extends AbstractTransformArrivingHandler {
    private IncomingMessageListener listener;

    public ReadToListenerHandler(IncomingMessageListener listener) {
        Validate.notNull(listener);

        this.listener = listener;
    }
    
    @Override
    protected Object transform(SocketAddress localAddress, SocketAddress remoteAddress, Object obj) {
        try {
            listener.newMessage(new IncomingMessage(localAddress, remoteAddress, obj));
        } catch (Exception e) {
            // do nothing
        }
        
        return obj;
    }
    
}
