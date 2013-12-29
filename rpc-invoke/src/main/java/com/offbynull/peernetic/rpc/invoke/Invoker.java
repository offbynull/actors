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

import java.io.Closeable;
import java.util.Map;

/**
 * Invokes methods on an object based on serialized data. See {@link Capturer}.
 * @param <T> type
 * @author Kasra Faghihi
 */
public interface Invoker<T> extends Closeable {

    /**
     * Invoke a method.
     * <p/>
     * May block until the invokation completes or may return right away.
     * @param data serialized invokation data
     * @param callback listener
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invoke(final byte[] data, final InvokerListener callback);

    /**
     * Invoke a method. This version of {@code invoke} allows adding in extra parameters to the invokation through the
     * {@link InvokeThreadInformation} class.
     * <p/>
     * May block until the invokation completes or may return right away.
     * @param data serialized invokation data
     * @param callback listener
     * @param info extra information to pass in to invokation
     * @throws NullPointerException if any arguments are {@code null}
     */
    void invoke(final byte[] data, final InvokerListener callback, Map<? extends Object, ? extends Object> info);
    
}
