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
package com.offbynull.peernetic.io.actors.udpsimulator;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * Message coming in for processing by {@link Line}.
 * @author Kasra Faghihi
 */
public final class DepartMessage {
    private final Object message;
    private final Address sourceAddress;
    private final Address destinationAddress;

    /**
     * Constructs a {@link DepartMessage} instance.
     * @param message message
     * @param sourceAddress relative source address of message (relative to calling actor)
     * @param destinationAddress destination address of message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destinationAddress} is empty
     */
    public DepartMessage(Object message, Address sourceAddress, Address destinationAddress) {
        Validate.notNull(message);
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        // sourceId can be empty
        Validate.isTrue(!destinationAddress.isEmpty());
        this.message = message;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }

    /**
     * Get message.
     * @return message
     */
    public Object getMessage() {
        return message;
    }

    /**
     * Get source address. The address returned by this method is to the calling actor's self address (relative to
     * {@link Context#getSelf()}).
     * @return source address
     */
    public Address getSourceAddress() {
        return sourceAddress;
    }

    /**
     * Get destination address.
     * @return destination address
     */
    public Address getDestinationAddress() {
        return destinationAddress;
    }

    
}
