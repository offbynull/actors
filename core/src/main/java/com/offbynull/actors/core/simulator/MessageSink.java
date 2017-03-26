/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.core.simulator;

import com.offbynull.actors.core.shuttle.Address;
import java.io.IOException;
import java.time.Instant;

/**
 * Write messages from the simulation to some external source.
 * @author Kasra Faghihi
 */
public interface MessageSink extends AutoCloseable {
    /**
     * Write the next message.
     * @param source source address (can be empty)
     * @param destination destination address
     * @param time time when message arrived
     * @param message message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty
     * @throws IOException if an error occurs while writing
     */
    void writeNextMessage(Address source, Address destination, Instant time, Object message) throws IOException;
}
