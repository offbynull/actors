package com.offbynull.p2prpc.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * A {@link MessageListener} implementation that calls a chain of message listeners.
 * @param <A> 
 */
public final class CompositeMessageListener<A> implements MessageListener<A> {
    
    private List<MessageListener<A>> callbacks;

    public CompositeMessageListener(MessageListener<A> ... callbacks) {
        this(Arrays.asList(callbacks));
    }
    public CompositeMessageListener(Collection<MessageListener<A>> callbacks) {
        Validate.noNullElements(callbacks);
        
        this.callbacks = new ArrayList<>(callbacks);
    }

    @Override
    public void messageArrived(A from, byte[] data, ResponseHandler responseCallback) {
        try {
            for (MessageListener<A> callback : callbacks) {
                callback.messageArrived(from, data, responseCallback);
            }
        } catch (RuntimeException re) {
            responseCallback.terminate();
            throw re;
        }
    }
    
}
