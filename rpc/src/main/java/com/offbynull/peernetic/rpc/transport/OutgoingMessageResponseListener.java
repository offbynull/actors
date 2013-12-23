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
 * A listener that gets triggered when a response arrives for an {@link OutgoingMessage}. Implementations must be thread-safe.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface OutgoingMessageResponseListener<A> {
    /**
     * Indicates that a response has arrived. Implementations of this method must not block.
     * @param response response
     * @throws NullPointerException if any arguments are {@code null}
     */
    void responseArrived(IncomingResponse<A> response);
    /**
     * Indicates that an internal error occurred. Implementations of this method must not block.
     * @param error throwable that caused the error (may be {@code null)}
     */
    void internalErrorOccurred(Throwable error);
    /**
     * Indicates that waiting for the response timed out. Implementations of this method must not block.
     */
    void timedOut();
}
