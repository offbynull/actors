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

import java.nio.ByteBuffer;

/**
 * A listener that gets triggered when a new message arrives.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface IncomingMessageListener<A> {
    /**
     * Indicates that a message has arrived.
     * Implementations must be thread-safe <b>AND MUST NEVER BLOCK</b>. It is your responsibility to avoid blocking. Blocking in this method
     * may block the underlying {@link Transport}.
     * @param from sender
     * @param message message (it is safe to hold on to this, you don't need to copy it)
     * @param responseCallback response handler
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    void messageArrived(A from, ByteBuffer message, IncomingMessageResponseListener responseCallback);
}