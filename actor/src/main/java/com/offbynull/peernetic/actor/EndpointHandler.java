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
package com.offbynull.peernetic.actor;

import java.util.Collection;

/**
 * Allows for pushing messages to a specific type of {@link Endpoint}.
 * @author Kasra Faghihi
 * @param <E> endpoint type
 */
public interface EndpointHandler<E extends Endpoint> {
    /**
     * Push messages from {@link source} to {@link destination}.
     * @param source the endpoint sending these messages -- replies should point back here
     * @param destination the endpoint receiving these messages
     * @param outgoing the messages being sent
     * @throws NullPointerException if any arguments are {@code null}
     */
    void push(Endpoint source, E destination, Collection<Outgoing> outgoing);
}
