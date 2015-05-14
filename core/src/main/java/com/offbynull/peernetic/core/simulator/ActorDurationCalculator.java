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
package com.offbynull.peernetic.core.simulator;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;

/**
 * Used to calculate the amount of time an {@link Actor} takes to process an incoming message when running in a simulation. Use this to
 * simulate conditions on a system (e.g. an actor running on a system that's under heavy load will take longer to process incoming
 * messages), or monitor how long actors are taking to execute (real execution durations are provided).
 * @author Kasra Faghihi
 */
public interface ActorDurationCalculator {
    /**
     * Calculates the amount of time an {@link Actor} takes to process an incoming message when running in a simulation.
     * @param source source address
     * @param destination destination address
     * @param message message being processed
     * @param realDuration real amount of time that this message took to process
     * @return amount of time that this message took in the simulation
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code realDuration} is negative, or {@code source} is empty, or {@code destination} is empty
     */
    Duration calculateDuration(Address source, Address destination, Object message, Duration realDuration);
}
