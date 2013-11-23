package com.offbynull.rpc.transport;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.Validate;

public final class ModifiableCompositeIncomingMessageListener<A> implements IncomingMessageListener<A> {
    private List<IncomingMessageListener<A>> listeners;

    public ModifiableCompositeIncomingMessageListener() {
        this(Collections.<IncomingMessageListener<A>>emptyList());
    }
    
    public ModifiableCompositeIncomingMessageListener(Collection<IncomingMessageListener<A>> listeners) {
        Validate.noNullElements(listeners);
        
        this.listeners = new CopyOnWriteArrayList<>(listeners);
    }

    public void add(IncomingMessageListener<A> e) {
        Validate.notNull(e);
        
        listeners.add(e);
    }

    public void remove(IncomingMessageListener<A> e) {
        Validate.notNull(e);
        
        listeners.remove(e);
    }

    public void addAll(Collection<? extends IncomingMessageListener<A>> c) {
        Validate.noNullElements(c);
        
        listeners.addAll(c);
    }

    public void addAll(int index, Collection<? extends IncomingMessageListener<A>> c) {
        Validate.noNullElements(c);
        
        listeners.addAll(index, c);
    }

    public void removeAll(Collection<? extends IncomingMessageListener<A>> c) {
        Validate.noNullElements(c);
        
        listeners.removeAll(c);
    }

    public void retainAll(Collection<? extends IncomingMessageListener<A>> c) {
        Validate.noNullElements(c);
        
        listeners.retainAll(c);
    }

    public void clear() {
        listeners.clear();
    }

    public void add(int index, IncomingMessageListener<A> e) {
        Validate.notNull(e);
        
        listeners.add(index, e);
    }

    public void remove(int index) {
        listeners.remove(index);
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
