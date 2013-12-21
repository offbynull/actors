/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.rpc.transport;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.Validate;

/**
 * A composite {@link IncomingMessageListener} that allows adding/removing of inner listeners.
 * @author Kasra F
 * @param <A> address type
 */
public final class ModifiableCompositeIncomingMessageListener<A> implements IncomingMessageListener<A> {
    private CopyOnWriteArrayList<IncomingMessageListener<A>> listeners;

    /**
     * Construct an empty {@link ModifiableCompositeIncomingMessageListener}.
     */
    public ModifiableCompositeIncomingMessageListener() {
        this(Collections.<IncomingMessageListener<A>>emptyList());
    }
    
    /**
     * Construct a {@link ModifiableCompositeIncomingMessageListener} populated with {@code listeners}.
     * @param listeners initial listeners
     */
    public ModifiableCompositeIncomingMessageListener(Collection<IncomingMessageListener<A>> listeners) {
        Validate.noNullElements(listeners);
        
        this.listeners = new CopyOnWriteArrayList<>(listeners);
    }

    /**
     * Add listeners to the start of the chain.
     * @param e listeners to add
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void addFirst(IncomingMessageListener<A> ... e) {
        Validate.noNullElements(e);
        
        listeners.addAll(0, Arrays.asList(e));
    }

    /**
     * Add listeners to the end of the chain.
     * @param e listeners to add
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void addLast(IncomingMessageListener<A> ... e) {
        Validate.noNullElements(e);
        
        listeners.addAll(Arrays.asList(e));
    }
    
    /**
     * Remove listeners from the chain.
     * @param e listeners remove
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void remove(IncomingMessageListener<A> ... e) {
        Validate.noNullElements(e);
        
        listeners.removeAll(Arrays.asList(e));
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
