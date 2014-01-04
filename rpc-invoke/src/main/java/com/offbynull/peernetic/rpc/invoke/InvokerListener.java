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
 * @author Kasra Faghihi
 */
public interface InvokerListener {
    /**
     * Indicates that the invocation failed.
     * @param t exception
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invocationFailed(Throwable t);
    /**
     * Indicates that the invocation was successful.
     * @param data serialized result or throwable
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invocationFinised(byte[] data);
}
