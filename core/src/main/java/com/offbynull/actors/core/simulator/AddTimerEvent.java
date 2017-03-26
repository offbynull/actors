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
package com.offbynull.actors.core.simulator;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class AddTimerEvent extends Event {
    
    private final String address;

    public AddTimerEvent(String address, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(address);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "AddTimerEvent{" + super.toString() + ", address=" + address + '}';
    }
    
}
