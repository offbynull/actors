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
package com.offbynull.peernetic.common.concurrent.pump;

import java.io.IOException;
import java.util.Collection;

/**
 * Writes messages to a message pump.
 * @author Kasra Faghihi
 * @param <T> message type
 */
public interface PumpWriter<T> {
    /**
     * Writes messages to a message pump.
     * @param data messages to write
     * @throws InterruptedException if interrupted (only thrown in underlying implementation can block)
     * @throws IOException if something went wrong while sending messages
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException may be thrown if the underlying mechanism used by the pump has been closed / is not available
     */
    void push(Collection<T> data) throws InterruptedException, IOException;

    /**
     * Writes messages to a message pump.
     * @param data messages to write
     * @throws InterruptedException if interrupted (only thrown in underlying implementation can block)
     * @throws IOException if something went wrong while sending messages
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException may be thrown if the underlying mechanism used by the pump has been closed / is not available
     */
    void push(T ... data) throws InterruptedException, IOException;
}
