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
package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;

/**
 * Async {@link UnstructuredService}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
interface UnstructuredServiceAsync<A> {
    /**
     * See {@link UnstructuredService#getState() }.
     * @param result see {@link UnstructuredService}
     */
    void getState(AsyncResultListener<State<A>> result);

    /**
     * See {@link UnstructuredService#join(byte[])  }.
     * @param result see {@link UnstructuredService}
     * @param secret see {@link UnstructuredService}
     */
    void join(AsyncResultListener<Boolean> result, byte[] secret);
    
    /**
     * See {@link UnstructuredService#keepAlive(byte[]) }.
     * @param result see {@link UnstructuredService}
     * @param secret see {@link UnstructuredService}
     */
    void keepAlive(AsyncResultListener<Boolean> result, byte[] secret);
}
