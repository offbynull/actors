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

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

/**
 * Message processed by {@code Line}.
 * @author Kasra Faghihi
 */
public final class TransitMessage {
    private Address sourceAddress;
    private Address destinationAddress;
    private Object message;
    private Instant departTime;
    private Duration duration;

    /**
     * Constructs a {@link TransitMessage} instance.
     * @param message message
     * @param sourceAddress relative source address of message (relative to calling actor)
     * @param destinationAddress destination address of message
     * @param departTime departure time
     * @param duration amount of time before reaching destination
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destinationAddress} is empty, or if {@code duration} is negative
     */
    public TransitMessage(Address sourceAddress, Address destinationAddress, Object message, Instant departTime, Duration duration) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.notNull(departTime);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        // sourceId can be empty
        Validate.isTrue(!destinationAddress.isEmpty());
        this.sourceAddress = sourceAddress;
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