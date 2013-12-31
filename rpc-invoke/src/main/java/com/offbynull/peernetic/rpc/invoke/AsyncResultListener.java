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
 * Receives the response from an async proxy method invokation.
 * @author Kasra Faghihi
 * @param <T> return type
 */
public interface AsyncResultListener<T> {
    /**
     * The method returned successfully.
     * @param object return object
     */
    void invokationReturned(T object);
    /**
     * The method threw an exception.
     * @param err exception
     * @throws NullPointerException if any arguments are {@code null}.
     */
    void invokationThrew(Throwable err);
    /**
     * Something went wrong in the invokation process.
     * @param err error object
     */
    void invokationFailed(Object err);
}