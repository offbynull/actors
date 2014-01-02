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
 * An abstract base-class for network transport implementations.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public abstract class Transport<A> extends Actor {

    private volatile IncomingFilter<A> incomingFilter = new NullIncomingFilter<>();
    private volatile OutgoingFilter<A> outgoingFilter = new NullOutgoingFilter<>();
    private volatile ActorQueueWriter writer;
    
    /**
     * Constructs a {@link Transport} object.
     * @param daemon daemon thread
     */
    public Transport(boolean daemon) {
        super(daemon);
    }

    public final void setWriter(ActorQueueWriter writer) {
        Validate.notNull(writer);
        Validate.validState(isNew());
        
        this.writer = writer;
    }

    public void setIncomingFilter(IncomingFilter<A> incomingFilter) {
        Validate.notNull(incomingFilter);
        Validate.validState(isNew());
        
        this.incomingFilter = incomingFilter;
    }

    public void setOutgoingFilter(OutgoingFilter<A> outgoingFilter) {
        Validate.notNull(outgoingFilter);
        Validate.validState(isNew());
        
        this.outgoingFilter = outgoingFilter;
    }
    
    public ActorQueueWriter getWriter() {
        return writer;
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
