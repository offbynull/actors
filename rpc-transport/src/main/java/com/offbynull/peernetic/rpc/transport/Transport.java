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
package com.offbynull.peernetic.rpc.transport;

import com.offbynull.peernetic.common.concurrent.actor.Actor;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullIncomingFilter;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullOutgoingFilter;
import org.apache.commons.lang3.Validate;

/**
 * An abstract base-class for network transport implementations. Actor-based and uses messages:
 * <ul>
 * <li>{@link SendRequestCommand}</li>
 * <li>{@link SendResponseCommand}</li>
 * <li>{@link DropResponseCommand}</li>
 * <li>{@link RequestArrivedEvent}</li>
 * <li>{@link ResponseArrivedEvent}</li>
 * <li>{@link ResponseErroredEvent}</li>
 * </ul>
 * @author Kasra Faghihi
 * @param <A> address type
 */
public abstract class Transport<A> extends Actor {

    private volatile IncomingFilter<A> incomingFilter = new NullIncomingFilter<>();
    private volatile OutgoingFilter<A> outgoingFilter = new NullOutgoingFilter<>();
    private volatile ActorQueueWriter dstWriter;
    
    /**
     * Constructs a {@link Transport} object.
     * @param daemon daemon thread
     */
    public Transport(boolean daemon) {
        super(daemon);
    }

    /**
     * Set the writer that this transport notifies of events. Can only be called before {@link #start() }.
     * @param writer writer to notify of events
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if called after {@link #start() }
     */
    public final void setDestinationWriter(ActorQueueWriter writer) {
        Validate.notNull(writer);
        Validate.validState(isNew());
        
        this.dstWriter = writer;
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
     * Get the writer others can use to write to this transport.
     * @return writer others can use to write to this transport
     */
    public final ActorQueueWriter getWriter() {
        return super.getSelfWriter();
    }
    
    /**
     * Get the writer to notify of events.
     * @return writer to notify of events
     */
    protected final ActorQueueWriter getDestinationWriter() {
        return dstWriter;
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


}
