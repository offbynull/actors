/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.network;

import java.nio.ByteBuffer;

/**
 * Modifies raw message data coming in to a {@link Transport}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface IncomingFilter<A> {
    /**
     * Modifies data.
     * @param from address the data is to/from
     * @param buffer data to be modified
     * @throws NullPointerException if any arguments are {@code null}
     * @return modified data
     */
    ByteBuffer filter(A from, ByteBuffer buffer); 
}
