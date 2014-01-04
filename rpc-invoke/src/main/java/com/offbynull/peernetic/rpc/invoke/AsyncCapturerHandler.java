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
 * Interface called by methods of async proxy objects generated through
 * {@link AsyncCapturer#createInstance(com.offbynull.rpc.invoke.AsyncCapturerHandler) }.
 * @author Kasra Faghihi
 */
public interface AsyncCapturerHandler {
    /**
     * Indicates that a method on an async proxy object was called.
     * @param data serialized invocation data
     * @param responseHandler called once a response is ready
     * @throws NullPointerException if any argument are {@code null}
     */
    void invocationTriggered(byte[] data, AsyncCapturerHandlerCallback responseHandler);
    /**
     * Indicates that a method invocation on an async proxy object failed.
     * @param err error thrown
     * @throws NullPointerException if any argument are {@code null}
     */
    void invocationFailed(Throwable err);
}
