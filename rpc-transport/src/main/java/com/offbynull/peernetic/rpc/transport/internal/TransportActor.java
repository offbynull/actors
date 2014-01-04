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
package com.offbynull.peernetic.rpc.transport.internal;

import com.offbynull.peernetic.common.concurrent.actor.Actor;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullIncomingFilter;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullOutgoingFilter;
import org.apache.commons.lang3.Validate;

/**
 * An abstract base-class for network transport implementations to use internally. Actor-based and uses messages:
 * <ul>
 * <li>{@link SendRequestCommand}</li>
 * <li>{@link SendResponseCommand}</li>
 * <li>{@link DropResponseCommand}</li>
 * </ul>
 * @author Kasra Faghihi
 * @param <A> address type
 */
public abstract class TransportActor<A> extends Actor {

    private volatile IncomingMessageListener<A> incomingMessageListener;
    private volatile IncomingFilter<A> incomingFilter = new NullIncomingFilter<>();
    private volatile OutgoingFilter<A> outgoingFilter = new NullOutgoingFilter<>();
    
    /**
     * Constructs a {@link Transport} object.
     * @param daemon daemon thread
     */
    public TransportActor(boolean daemon) {
        super(daemon);
    }
    
    /**
     * Set the incoming message listener. Can only be called before {@link #start() }.
     * @param incomingMessageListener incoming filter
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if called after {@link #start() }
     */
    public final void setIncomingMessageListener(IncomingMessageListener<A> incomingMessageListener) {
        Validate.notNull(incomingMessageListener);
        Validate.validState(isNew());
        
        this.incomingMessageListener = incomingMessageListener;
    }
    
    /**
     * Set the incoming filter. Can only be called before {@link #start() }.
     * @param incomingFilter incoming filter
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if called after {@link #start() }
     */
    public final void setIncomingFilter(IncomingFilter<A> incomingFilter) {
        Validate.notNull(incomingFilter);
        Validate.validState(isNew());
        
        this.incomingFilter = incomingFilter;
    }

    /**
     * Set the outgoing filter. Can only be called before {@link #start() }.
     * @param outgoingFilter incoming filter
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if called after {@link #start() }
     */
    public final void setOutgoingFilter(OutgoingFilter<A> outgoingFilter) {
        Validate.notNull(outgoingFilter);
        Validate.validState(isNew());
        
        this.outgoingFilter = outgoingFilter;
    }

    /**
     * Get the incoming message listener for this transport.
     * @return incoming message listener
     */
    public final IncomingMessageListener<A> getIncomingMessageListener() {
        return incomingMessageListener;
    }
    
    /**
     * Get the incoming filter for this transport.
     * @return incoming filter
     */
    protected final IncomingFilter<A> getIncomingFilter() {
        return incomingFilter;
    }

    /**
     * Get the outgoing filter for this transport.
     * @return outgoing filter
     */
    protected final OutgoingFilter<A> getOutgoingFilter() {
        return outgoingFilter;
    }

    /**
     * Public version of {@link #getSelfWriter() }.
     * @return writer
     */
    public final ActorQueueWriter getInternalWriter() {
        return getSelfWriter();
    }


}
