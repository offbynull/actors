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
package com.offbynull.peernetic.core.simulator;

import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.SourceContext;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class ActorHolder implements Holder {
    private final Address address;
    private final CoroutineRunner actorRunner;
    private final Duration timeOffset;
    private final SourceContext context;
    
    private Instant earliestPossibleOnStepTime;

    public ActorHolder(Address address, CoroutineRunner actorRunner, Duration timeOffset, Instant earliestPossibleOnStepTime) {
        Validate.notNull(address);
        Validate.notNull(actorRunner);
        Validate.notNull(timeOffset);
        Validate.notNull(earliestPossibleOnStepTime);
        Validate.isTrue(!address.isEmpty());
        Validate.isTrue(!timeOffset.isNegative());
        this.address = address;
        this.actorRunner = actorRunner;
        this.timeOffset = timeOffset;
        this.context = new SourceContext();
        this.earliestPossibleOnStepTime = earliestPossibleOnStepTime;
    }

    public CoroutineRunner getActorRunner() {
        return actorRunner;
    }

    public Duration getTimeOffset() {
        return timeOffset;
    }

    @Override
    public Address getAddress() {
        return address;
    }

    public SourceContext getContext() {
        return context;
    }

    public Instant getEarliestPossibleOnStepTime() {
        return earliestPossibleOnStepTime;
    }

    public void setEarliestPossibleOnStepTime(Instant earliestPossibleOnStepTime) {
        Validate.notNull(earliestPossibleOnStepTime);
        Validate.isTrue(!earliestPossibleOnStepTime.isBefore(this.earliestPossibleOnStepTime));
        this.earliestPossibleOnStepTime = earliestPossibleOnStepTime;
    }
}
