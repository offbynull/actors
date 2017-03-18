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

import com.offbynull.coroutines.user.Coroutine;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

final class AddActorEvent extends Event {

    private final String address;
    private final Coroutine actor;
    private final Duration timeOffset;
    private final UnmodifiableList<Object> primingMessages;

    public AddActorEvent(String address, Coroutine actor, Duration timeOffset, Instant triggerTime, long sequenceNumber,
            Object... primingMessages) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(address);
        Validate.notNull(actor);
        Validate.notNull(timeOffset);
        Validate.notNull(primingMessages);
        Validate.isTrue(!timeOffset.isNegative());
        Validate.noNullElements(primingMessages);

        this.address = address;
        this.actor = actor;
        this.timeOffset = timeOffset;
        this.primingMessages = (UnmodifiableList<Object>) UnmodifiableList.unmodifiableList(Arrays.asList(primingMessages));
    }

    public String getAddress() {
        return address;
    }

    public Coroutine getActor() {
        return actor;
    }

    public Duration getTimeOffset() {
        return timeOffset;
    }

    public UnmodifiableList<Object> getPrimingMessages() {
        return primingMessages;
    }

    @Override
    public String toString() {
        return "AddActorEvent{" + super.toString() + ", address=" + address + ", actor=" + actor + ", timeOffset=" + timeOffset
                + ", primingMessages=" + primingMessages + '}';
    }

}
