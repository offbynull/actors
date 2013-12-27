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
 * Receives notifications when a link is established/destroyed with other nodes.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface LinkManagerListener<A> {
    /**
     * Link has been established.
     * @param linkManager link manager
     * @param type link type
     * @param address address
     * @throws NullPointerException if any arguments are {@code null}
     */
    void linkCreated(LinkManager<A> linkManager, LinkType type, A address);
    
    /**
     * Link has been closed.
     * @param linkManager link manager
     * @param type link type
     * @param address address
     * @throws NullPointerException if any arguments are {@code null}
     */
    void linkDestroyed(LinkManager<A> linkManager, LinkType type, A address);

    /**
     * Attempted to create new outgoing links but address cache is empty.
     * @param linkManager link manager
     * @throws NullPointerException if any arguments are {@code null}
     */
    void addressCacheEmpty(LinkManager<A> linkManager);
}
