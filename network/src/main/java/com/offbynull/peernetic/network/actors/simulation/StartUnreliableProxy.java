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
import org.apache.commons.lang3.Validate;

public class StartUnreliableProxy {
    private final Address timerPrefix;
    private final Address actorPrefix;
    private final Line line;

    public StartUnreliableProxy(Address timerPrefix, Address actorPrefix, Line line) {
        Validate.notNull(timerPrefix);
        Validate.notNull(actorPrefix);
        Validate.notNull(line);
        Validate.isTrue(!timerPrefix.isEmpty());
        Validate.isTrue(!actorPrefix.isEmpty());
        Validate.isTrue(!timerPrefix.equals(actorPrefix));
        this.timerPrefix = timerPrefix;
        this.actorPrefix = actorPrefix;
        this.line = line;
    }

    public Address getTimerPrefix() {
        return timerPrefix;
    }

    public Address getActorPrefix() {
        return actorPrefix;
    }

    public Line getLine() {
        return line;
    }
}
