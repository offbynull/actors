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
package com.offbynull.peernetic.rpc.transport.transports.test;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullIncomingFilter;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.internal.TransportActor;
import com.offbynull.peernetic.rpc.transport.internal.SendRequestCommand;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * A {@link TransportActor} used for testing. Backed by a {@link TestHub}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class TestTransport<A> implements Transport<A> {

    private TestTransportActor<A> transportActor;
    private ActorQueueWriter writer;

    /**
     * Constructs a {@link TestTransport} object.
     * @param address address to listen on
     * @param cacheSize number of packet ids to cache
     * @param outgoingResponseTimeout timeout duration for responses for outgoing requests to arrive
     * @param incomingResponseTimeout timeout duration for responses for incoming requests to be processed
     * @param hub test hub
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     * @throws NullPointerException if any arguments are {@code null}
     */
    public TestTransport(A address, int cacheSize, long outgoingResponseTimeout,
                long incomingResponseTimeout, TestHub<A> hub) {
        Validate.notNull(address);
        Validate.notNull(hub);
        
        transportActor = new TestTransportActor<>(address, cacheSize, outgoingResponseTimeout, incomingResponseTimeout,
                hub.getInternalWriter());
        writer = transportActor.getInternalWriter();
    }
    
    
    @Override
    public void start(IncomingMessageListener<A> listener) throws IOException {
        start(new NullIncomingFilter<A>(), listener, new NullOutgoingFilter<A>());
    }

    @Override
    public void start(IncomingFilter<A> incomingFilter, IncomingMessageListener<A> listener, OutgoingFilter<A> outgoingFilter) {
        Validate.notNull(incomingFilter);
        Validate.notNull(listener);
        Validate.notNull(outgoingFilter);
        
        transportActor.setIncomingFilter(incomingFilter);
        transportActor.setOutgoingFilter(outgoingFilter);
        transportActor.setIncomingMessageListener(listener);
        
        transportActor.start();
    }

    @Override
    public void stop() {
        transportActor.stop();
    }

    @Override
    public void sendMessage(A to, ByteBuffer message, OutgoingMessageResponseListener listener) {
        writer.push(Message.createOneWayMessage(new SendRequestCommand(to, message, listener)));
    }
}
