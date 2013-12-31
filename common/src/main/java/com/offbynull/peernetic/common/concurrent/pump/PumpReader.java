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
import java.util.Iterator;

/**
 * Reads messages from a message pump.
 * @author Kasra Faghihi
 */
public interface PumpReader {
    /**
     * Blocks until messages are available, and then returns them.
     * @param timeout amount of time to block
     * @return an iterator returning messages (potentially empty), will not contain {@code null}s
     * @throws InterruptedException if interrupted
     * @throws IOException if something went wrong while getting messages
     * @throws IllegalArgumentException if any numeric argument is negative
     * @throws IllegalStateException may be thrown if the underlying mechanism used by the pump has been closed / is not available
     */
    Iterator<Message> pull(long timeout) throws InterruptedException, IOException;
}
