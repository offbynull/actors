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
package com.offbynull.peernetic.rpc.transport.transports.udp;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullIncomingFilter;
import com.offbynull.peernetic.rpc.transport.filters.nil.NullOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.internal.SendRequestCommand;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * A UDP transport implementation.
 * @author Kasra Faghihi
 */
public final class UdpTransport implements Transport<InetSocketAddress> {
    
    private UdpTransportActor transportActor;
    private ActorQueueWriter writer;

    /**
     * Constructs a {@link UdpTransport} object.
     * @param listenAddress address to listen on
     * @param bufferSize buffer size
     * @param cacheSize number of packet ids to cache
     * @param packetFlushTimeout timeout duration for outgoing packets to get flushed in to the selector
     * @param outgoingResponseTimeout timeout duration for responses for outgoing requests to arrive
     * @param incomingResponseTimeout timeout duration for responses for incoming requests to be processed
     * @throws IOException on error
     * @throws IllegalArgumentException if any numeric argument is non-positive (less than 1)
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UdpTransport(InetSocketAddress listenAddress, int bufferSize, int cacheSize, long packetFlushTimeout,
            long outgoingResponseTimeout, long incomingResponseTimeout) throws IOException {
        Validate.notNull(listenAddress);
        
        transportActor = new UdpTransportActor(listenAddress, bufferSize, cacheSize, packetFlushTimeout, outgoingResponseTimeout,
                incomingResponseTimeout);
        writer = transportActor.getInternalWriter();
    }

    @Override
    public void start(IncomingMessageListener<InetSocketAddress> listener) throws IOException {
        start(new NullIncomingFilter<InetSocketAddress>(), listener, new NullOutgoingFilter<InetSocketAddress>());
    }

    @Override
    public void start(IncomingFilter<InetSocketAddress> incomingFilter, IncomingMessageListener<InetSocketAddress> listener,
            OutgoingFilter<InetSocketAddress> outgoingFilter) {
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
    public void sendMessage(InetSocketAddress to, ByteBuffer message, OutgoingMessageResponseListener listener) {
        writer.push(Message.createOneWayMessage(new SendRequestCommand(to, message, listener)));
    }
}
