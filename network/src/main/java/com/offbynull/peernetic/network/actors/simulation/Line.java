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
package com.offbynull.peernetic.network.actors.simulation;

import java.time.Instant;
import java.util.Collection;

/**
 * Interface for mimicking network conditions. For example, depending on the implementation, a message may be dropped, duplicate, corrupted,
 * slowed, etc.
 *
 * @author Kasra Faghihi
 */
public interface Line {
    
    /**
     * Called when an outgoing message should be processed by this line.
     *
     * @param time time of departure
     * @param departMessage outgoing message
     * @return list of {@link TransitMessage} objects generated from {@code departMessage}
     */
    Collection<TransitMessage> processOutgoing(Instant time, DepartMessage departMessage);

    /**
     * Called when an incoming message should be processed by this line.
     *
     * @param time time of departure
     * @param departMessage outgoing message
     * @return list of {@link TransitMessage} objects generated from {@code departMessage}
     */
    Collection<TransitMessage> processIncoming(Instant time, DepartMessage departMessage);

}
