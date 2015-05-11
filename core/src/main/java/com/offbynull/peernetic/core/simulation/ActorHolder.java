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
package com.offbynull.peernetic.core.simulation;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.SourceContext;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class ActorHolder implements Holder {
    private final String address;
    private final Actor actor;
    private final Duration timeOffset;
    private final SourceContext context;
    
    private Instant earliestPossibleOnStepTime;

    public ActorHolder(String address, Actor actor, Duration timeOffset, Instant earliestPossibleOnStepTime) {
        Validate.notNull(address);
        Validate.notNull(actor);
        Validate.notNull(timeOffset);
        Validate.notNull(earliestPossibleOnStepTime);
        Validate.isTrue(!timeOffset.isNegative());
        this.address = address;
        this.actor = actor;
        this.timeOffset = timeOffset;
        this.context = new SourceContext();
        this.earliestPossibleOnStepTime = earliestPossibleOnStepTime;
    }

    public Actor getActor() {
        return actor;
    }

    public Duration getTimeOffset() {
        return timeOffset;
    }

    public String getAddress() {
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
