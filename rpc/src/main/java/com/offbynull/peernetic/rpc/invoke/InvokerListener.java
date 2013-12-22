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
 * A listener that gets triggered once a method invoked through {@link Invoker} has finished.
 * @author Kasra F
 */
public interface InvokerListener {
    /**
     * Indicates that the invokation failed.
     * @param t exception
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invokationFailed(Throwable t);
    /**
     * Indicates that the invokation was successful.
     * @param data serialized result or throwable
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invokationFinised(byte[] data);
}
