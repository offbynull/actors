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
 * Provides the ability to proxy a non-final class or interface such that method invokations are processed by some external source.
 * Invokations are sent to the external source as a serialized byte array, and each invokation waits for the external source to give back
 * either a result to be returned by the invokation or a {@link Throwable} to be thrown from the invokation. {@link CapturerHandler} is the
 * processing mechanism.
 * @author Kasra Faghihi
 * @param <T> proxy type
 */
public interface Capturer<T> {

    /**
     * Creates a proxy object.
     * @param callback callback to notify when a method's been invoked on the returned proxy object
     * @return proxy object
     * @throws NullPointerException if any arguments are {@code null}
     */
    T createInstance(final CapturerHandler callback);
    
}
