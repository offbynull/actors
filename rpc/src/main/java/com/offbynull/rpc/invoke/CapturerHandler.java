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
package com.offbynull.rpc.invoke;

/**
 * Interface called by methods of proxy objects generated through
 * {@link Capturer#createInstance(com.offbynull.rpc.invoke.CapturerHandler) }.
 * @author Kasra F
 */
public interface CapturerHandler {
    /**
     * Indicates that a method on a proxy object was called.
     * @param data serialized invokation data
     * @return serialized result data
     * @throws NullPointerException if any argument are {@code null}
     */
    byte[] invokationTriggered(byte[] data);
    /**
     * Indicates that a method invokation on a proxy object failed.
     * @param err error thrown
     * @throws NullPointerException if any argument are {@code null}
     */
    void invokationFailed(Throwable err);
}
