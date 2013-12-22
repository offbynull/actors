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

/**
 * A listener that gets triggered when a new message arrives. Implementations must be thread-safe.
 * @author Kasra F
 * @param <A> address type
 */
public interface IncomingMessageListener<A> {
    /**
     * Indicates that a message has arrived. Implementations of this method must not block.
     * @param message message
     * @param responseCallback response handler
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback);
}
