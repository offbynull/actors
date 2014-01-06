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
 * An entity that can receive messages.
 * @author Kasra Faghihi
 */
public interface Endpoint {
    /**
     * Push messages from {@link source} to this endpoint.
     * @param source the endpoint sending these messages -- replies should point back here
     * @param outgoing the messages being sent
     * @throws NullPointerException if any arguments are {@code null}
     */
    void push(Endpoint source, Collection<Outgoing> outgoing);

    /**
     * Push messages from {@link source} to this endpoint.
     * @param source the endpoint sending these messages -- replies should point back here
     * @param outgoing the messages being sent
     * @throws NullPointerException if any arguments are {@code null}
     */
    void push(Endpoint source, Outgoing ... outgoing);
}
