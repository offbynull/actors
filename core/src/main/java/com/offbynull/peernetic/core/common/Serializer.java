/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.common;

/**
 * Interface to serialize objects to a byte array and deserialize objects.
 * @author Kasra Faghihi
 */
public interface Serializer {
    /**
     * Serialize an object array in to a byte array.
     * @param obj object to serialize
     * @return serialized data
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException on serialization error
     */
    byte[] serialize(Object obj);
    /**
     * Deserializes an object from a byte array.
     * @param <T> return type
     * @param data data to deserialize
     * @return deserialized object
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException on serialization error
     */
    <T> T deserialize(byte[] data);
}
