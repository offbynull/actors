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
package com.offbynull.peernetic.network.actors.udpsimulator;

import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

/**
 * Message processed by {@code Line}.
 * @author Kasra Faghihi
 */
public final class TransitMessage {
    private Address sourceId;
    private Address destinationAddress;
    private Object message;
    private Instant departTime;
    private Duration duration;

    /**
     * Constructs a {@link TransitMessage} instance.
     * @param message message
     * @param sourceId source id of message
     * @param destinationAddress destination address of message
     * @param departTime departure time
     * @param duration amount of time before reaching destination
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destinationAddress} is empty, or if {@code duration} is negative
     */
    public TransitMessage(Address sourceId, Address destinationAddress, Object message, Instant departTime, Duration duration) {
        Validate.notNull(sourceId);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.notNull(departTime);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        // sourceId can be empty
        Validate.isTrue(!destinationAddress.isEmpty());
        this.sourceId = sourceId;
        this.destinationAddress = destinationAddress;
        this.message = message;
        this.departTime = departTime;
        this.duration = duration;
    }

    /**
     * Get message.
     * @return message
     */
    public Object getMessage() {
        return message;
    }

    /**
     * Get source id.
     * @return source id
     */
    public Address getSourceId() {
        return sourceId;
    }

    /**
     * Get destination address.
     * @return destination address
     */
    public Address getDestinationAddress() {
        return destinationAddress;
    }
    
    /**
     * Get departure time.
     * @return departure time
     */
    Instant getDepartTime() {
        return departTime;
    }

    /**
     * Get duration of time before this message is to reach its destination.
     * @return duration of time until this message reaches its destination
     */
    Duration getDuration() {
        return duration;
    }
}