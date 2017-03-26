/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.core.shuttle;

import java.util.Collection;

/**
 * A {@link Shuttle} transports {@link Message}s to a specific actor or gateway. Shuttles provide no reliability guarantees -- meaning that
 * messages may arrive delayed, out-of-order, or not at all. Reliability depends on the implementation and the underlying transport
 * mechanism used for message passing. For example, messages that are being passed locally will always arrive in a timely manner and
 * in-order, while messages passed over UDP may not.
 * @author Kasra Faghihi
 */
public interface Shuttle {
    /**
     * Address prefix this shuttle is for.
     * @return address prefix
     */
    String getPrefix();
    /**
     * Sends {@link Message}s to the specific actor or gateway this {@link Shuttle} is for. Each message's destination
     * address must contain the same prefix as that returned by {@link #getPrefix() }, otherwise that message will be silently discarded.
     * @param messages messages to send
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    void send(Collection<Message> messages);
}
