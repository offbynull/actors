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
 * A listener that gets triggered when a response arrives for an outgoing message.
 * @author Kasra Faghihi
 */
public interface OutgoingMessageResponseListener {
    /**
     * Indicates that a response has arrived.
     * Implementations must be thread-safe <b>AND MUST NEVER BLOCK</b>. It is your responsibility to avoid blocking. Blocking in this method
     * may block the underlying {@link Transport}.
     * @param response response (it is safe to hold on to this, you don't need to copy it)
     * @throws NullPointerException if any arguments are {@code null}
     */
    void responseArrived(ByteBuffer response);
    /**
     * Indicates that an internal error occurred.
     * Implementations must be thread-safe <b>AND MUST NEVER BLOCK</b>. It is your responsibility to avoid blocking. Blocking in this method
     * may block the underlying {@link Transport}.
     * @param error exception or other object that caused or describes the error -- may be {@code nul}) (it is safe to hold on to this, you
     * don't need to copy it)
     */
    void errorOccurred(Object error);
}