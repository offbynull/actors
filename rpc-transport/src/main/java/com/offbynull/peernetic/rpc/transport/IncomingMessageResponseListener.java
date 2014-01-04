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
 * A handler that should be triggered once a response is ready for an incoming message.
 * @author Kasra Faghihi
 */
public interface IncomingMessageResponseListener {
    /**
     * Indicates that a response is ready to go out.
     * Implementations <b>MUST NEVER BLOCK</b>. It is your responsibility to avoid blocking.
     * @param response response to send out (caller still owns the reference to this, a copy is made by the callee)
     * @throws NullPointerException if any arguments are {@code null}
     */
    void responseReady(ByteBuffer response);
    /**
     * Indicates that a response shouldn't be sent out.
     * Implementations must be thread-safe <b>AND MUST NEVER BLOCK</b>. It is your responsibility to avoid blocking.
     */
    void terminate();
}