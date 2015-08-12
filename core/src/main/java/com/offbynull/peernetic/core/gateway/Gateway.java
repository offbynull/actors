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
package com.offbynull.peernetic.core.gateway;

import com.offbynull.peernetic.core.actor.Actor;

/**
 * A {@link Gateway}, like an {@link Actor}, communicates with other components through message-passing, but isn't
 * bound by any of the same rules as {@link Actor}s. Gateways are mainly used to interface with third-party components that can't be
 * communicated via message-passing. As such, it's perfectly acceptable for a gateway to expose internal state, share state, perform
 * I/O, perform thread synchronization, or otherwise block.
 * 
 * For example, a gateway could ...
 * 
 * <ul>
 * <li>read messages from a TCP connection and forward them.</li>
 * <li>read messages from a file and forward them.</li>
 * <li>receive messages and write them to a file.</li>
 * <li>visualize incoming messages using Swing or JavaFX.</li>
 * </ul>
 * 
 * @author Kasra Faghihi
 */
public interface Gateway extends AutoCloseable {
    
}
