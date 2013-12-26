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
package com.offbynull.peernetic.overlay.unstructured;

/**
 * RPC service for an unstructured overlay.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface UnstructuredService<A> {
    /**
     * Service ID.
     */
    int SERVICE_ID = 5000;
    /**
     * Size of a secret value used for establishing and maintaining links.
     */
    int SECRET_SIZE = 16;

    /**
     * Get this node's state.
     * @return this node's state
     */
    State<A> getState();

    /**
     * Attempt to link to this node. If successful, the caller must call {@link #keepAlive(byte[]) } periodically to ensure that the new
     * link stays up.
     * @param secret secret to use for authentication during calls to {@link #keepAlive(byte[]) }
     * @return {@code true} if successful, {@code false} otherwise
     */
    boolean join(byte[] secret);
    
    /**
     * Attempt to keep alive a link.
     * @param secret secret for link
     * @return {@code true} if successful, {@code false} if link not found
     */
    boolean keepAlive(byte[] secret);
}
