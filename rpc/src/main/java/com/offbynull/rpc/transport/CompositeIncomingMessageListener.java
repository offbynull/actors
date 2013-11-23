package com.offbynull.rpc.transport;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class CompositeIncomingMessageListener<A> implements IncomingMessageListener<A> {
    private List<IncomingMessageListener<A>> listeners;

    public CompositeIncomingMessageListener(List<IncomingMessageListener<A>> listeners) {
        Validate.noNullElements(listeners);
        
        this.listeners = new ArrayList<>(listeners);
    }
    

    @Override
    public void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback) {
        try {
            for (IncomingMessageListener<A> listener : listeners) {
                listener.messageArrived(message, responseCallback);
            }
        } catch (RuntimeException re) {
            // do nothing
        }
    }
    
}
