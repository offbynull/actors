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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An interface to send, receive, and reply to messages over a network. Implementations must be thread-safe.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface Transport<A> {
    /**
     * Starts the transport. Equivalent to calling {@code start(new NullIncomingFilter(), listener, new NullOutgoingFilter())}.
     * @param listener listener for incoming messages
     * @throws IOException on error
     * @throws IllegalStateException if already started or stopped
     * @throws NullPointerException if any arguments are {@code null}
     */
    void start(IncomingMessageListener<A> listener) throws IOException;

    /**
     * Starts the transport.
     * @param incomingFilter incoming filter
     * @param listener listener for incoming messages
     * @param outgoingFilter outgoing filter
     * @throws IllegalStateException if already running or stopped, or fails to start up for whatever reason
     * @throws NullPointerException if any arguments are {@code null}
     */
    void start(IncomingFilter<A> incomingFilter, IncomingMessageListener<A> listener, OutgoingFilter<A> outgoingFilter);

    /**
     * Stops the transport. Cannot be restarted once stopped.
     */
    void stop();
    
    /**
     * Queues a message to be sent out. The behaviour of this method is undefined if the transport isn't in a started state. Implementations
     * of this method must not block.
     * @param to recipient
     * @param message message contents
     * @param listener handles message responses
     * @throws NullPointerException if any arguments are {@code null}
     */
    void sendMessage(A to, ByteBuffer message, OutgoingMessageResponseListener listener);
}
