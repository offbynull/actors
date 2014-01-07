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

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullIncomingFilter;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.serializers.xstream.XStreamDeserializer;
import com.offbynull.peernetic.rpc.transport.serializers.xstream.XStreamSerializer;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.Validate;

/**
 * An abstract base-class for network transport implementations.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public abstract class Transport<A> extends Actor {
    protected static final Object INCOMING_FILTER_KEY = IncomingFilter.class.getSimpleName();
    protected static final Object OUTGOING_FILTER_KEY = OutgoingFilter.class.getSimpleName();
    protected static final Object ENDPOINT_ROUTE_KEY = Endpoint.class.getSimpleName();
    protected static final Object SERIALIZER_KEY = Serializer.class.getSimpleName();
    protected static final Object DESERIALIZER_KEY = Deserializer.class.getSimpleName();
    
    /**
     * Constructs a {@link Transport} object.
     */
    public Transport() {
        putInStartupMap(INCOMING_FILTER_KEY, new NullIncomingFilter<A>());
        putInStartupMap(OUTGOING_FILTER_KEY, new NullOutgoingFilter<A>());
        putInStartupMap(ENDPOINT_ROUTE_KEY, new NullEndpoint());
        
        XStream xstream = new XStream();
        putInStartupMap(SERIALIZER_KEY, new XStreamSerializer(xstream));
        putInStartupMap(DESERIALIZER_KEY, new XStreamDeserializer(xstream));
    }
    
    /**
     * Set the incoming filter. Can only be called before {@link #start() }.
     * @param incomingFilter incoming filter
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if called after starting service
     */
    public final void setIncomingFilter(IncomingFilter<A> incomingFilter) {
        Validate.notNull(incomingFilter);
        putInStartupMap(INCOMING_FILTER_KEY, incomingFilter);
    }

    /**
     * Set the outgoing filter. Can only be called before {@link #start() }.
     * @param outgoingFilter incoming filter
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if called after starting service
     */
    public final void setOutgoingFilter(OutgoingFilter<A> outgoingFilter) {
        Validate.notNull(outgoingFilter);
        putInStartupMap(OUTGOING_FILTER_KEY, outgoingFilter);
    }

    /**
     * Set the endpoint to receive messages coming in to the transport. Can only be called before {@link #start() }.
     * @param endpoint incoming messages to the transport will go here
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if called after starting service
     */
    public final void setDestinationEndpoint(Endpoint endpoint) {
        Validate.notNull(endpoint);
        putInStartupMap(ENDPOINT_ROUTE_KEY, endpoint);        
    }
}
