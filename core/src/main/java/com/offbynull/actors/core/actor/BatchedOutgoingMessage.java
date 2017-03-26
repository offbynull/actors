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
package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * A queued outgoing message.
 * @author Kasra Faghihi
 */
public final class BatchedOutgoingMessage {
    private final Address source;
    private final Address destination;
    private final Object message;

    BatchedOutgoingMessage(Address source, Address destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.isTrue(!destination.isEmpty());
        this.source = source;
        this.destination = destination;
        this.message = message;
    }

    /**
     * Source address of the outgoing message.
     * @return source address
     */
    public Address getSource() {
        return source;
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
