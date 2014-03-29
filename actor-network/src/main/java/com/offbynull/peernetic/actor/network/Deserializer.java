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
 * Interface for deserializing messages.
 * @author Kasra Faghihi
 */
public interface Deserializer {
    /**
     * Deserialize an object.
     * @param data data to convert back to object
     * @return {@code data} deserialized to an object
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code data} deserializes to {@code null}
     * @throws IllegalStateException if anything goes wrong with the underlying deserializer
     */
    Object deserialize(ByteBuffer data);
}
