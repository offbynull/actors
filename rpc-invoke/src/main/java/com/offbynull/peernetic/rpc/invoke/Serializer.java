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
package com.offbynull.peernetic.rpc.invoke;

/**
 * Interface for serializing method invokations and invokation results.
 * @author Kasra Faghihi
 */
public interface Serializer {
    /**
     * Serialize method invokation.
     * @param invokeData invokation data
     * @return serialized method invokation
     * @throws NullPointerException if any arguments are {@code null}
     */
    byte[] serializeMethodCall(InvokeData invokeData);
    /**
     * Serialize method return value.
     * @param ret return value (may be {@code null})
     * @return serialized return value
     */
    byte[] serializeMethodReturn(Object ret);
    /**
     * Serialize method thrown exception.
     * @param err exception
     * @return serialized exception
     * @throws NullPointerException if any arguments are {@code null}
     */
    byte[] serializeMethodThrow(Throwable err);
}
