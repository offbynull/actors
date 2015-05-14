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
package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * A queued outgoing message.
 * @author Kasra Faghihi
 */
public final class BatchedOutgoingMessage {
    private final Address sourceId;
    private final Address destination;
    private final Object message;

    BatchedOutgoingMessage(Address sourceId, Address destination, Object message) {
        Validate.notNull(destination);
        Validate.notNull(message);
        // sourceId may be empty
        Validate.isTrue(destination.size() > 0);
        this.sourceId = sourceId;
        this.destination = destination;
        this.message = message;
    } // sourceId may be null

    /**
     * Source id of the outgoing message.
     * @return source id
     */
    public Address getSourceId() {
        return sourceId;
    }

    /**
     * Destination address of the outgoing message.
     * @return destination address
     */
    public Address getDestination() {
        return destination;
    }

    /**
     * Outgoing message.
     * @return outgoing message
     */
    public Object getMessage() {
        return message;
    }
    
}
