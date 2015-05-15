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

import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class TransitMessage {
    private Address sourceId;
    private Address destinationAddress;
    private Object message;
    private Instant departTime;
    private Duration duration;

    TransitMessage(Address sourceId, Address destinationAddress, Object message, Instant departTime, Duration duration) {
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

    Address getSourceId() {
        return sourceId;
    }

    Address getDestinationAddress() {
        return destinationAddress;
    }

    Object getMessage() {
        return message;
    }

    Instant getDepartTime() {
        return departTime;
    }

    Duration getDuration() {
        return duration;
    }
}