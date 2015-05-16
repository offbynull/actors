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
import java.util.function.Supplier;
import org.apache.commons.lang3.Validate;

/**
 * Priming message for {@link UdpSimulatorCoroutine}.
 * @author Kasra Faghihi
 */
public final class StartUdpSimulator {
    private final Address timerPrefix;
    private final Address actorPrefix;
    private final Supplier<Line> lineFactory;

    /**
     * Constructs a {@link StartUdpSimulator} instance.
     * @param timerPrefix address of timer
     * @param actorPrefix address of the actor the UDP simulator is for
     * @param lineFactory factory that creates new {@link Line}s
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code timerPrefix} or {@code actorPrefix} is empty
     */
    public StartUdpSimulator(Address timerPrefix, Address actorPrefix, Supplier<Line> lineFactory) {
        Validate.notNull(timerPrefix);
        Validate.notNull(actorPrefix);
        Validate.notNull(lineFactory);
        Validate.isTrue(!timerPrefix.isEmpty());
        Validate.isTrue(!actorPrefix.isEmpty());
        this.timerPrefix = timerPrefix;
        this.actorPrefix = actorPrefix;
        this.lineFactory = lineFactory;
    }

    Address getTimerPrefix() {
        return timerPrefix;
    }

    Address getActorPrefix() {
        return actorPrefix;
    }

    Supplier<Line> getLineFactory() {
        return lineFactory;
    }
}
