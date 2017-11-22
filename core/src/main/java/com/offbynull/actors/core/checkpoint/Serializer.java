/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.core.checkpoint;

import com.offbynull.actors.core.actor.Context;

/**
 * Interface to serialize and unserialize {@link Context}.
 * @author Kasra Faghihi
 */
public interface Serializer {

    /**
     * Serialize context.
     * @param ctx context
     * @return serialized context
     * @throws IllegalArgumentException if cannot be serialized for some reason
     * @throws NullPointerException if any argument is {@code null}
     */
    byte[] serialize(Context ctx);
    
    /**
     * Deserialize context.
     * @param data serialized context
     * @return context
     * @throws IllegalArgumentException if cannot be serialized for some reason
     * @throws NullPointerException if any argument is {@code null}
     */
    Context deserialize(byte[] data);
}
