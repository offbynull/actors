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

import java.time.Duration;

/**
 * Used to calculate the amount of time it takes to pass messages between two addresses in a simulation. Use this to simulate conditions on
 * a system (e.g. a message coming in to an actor that already has a large number of incoming messages buffered will may sit for a period of
 * time before getting processed by the actor).
 * @author Kasra Faghihi
 */
public interface MessageBehaviourDriver {
    // NOTE: unlike ActorBehaviourDriver, it doesn't make sense for this method to have a realDuration parameter. The way the simulator
    // works, we will never have access to an duration value for message passing, as messages are passed "instantly" in the simulator.
    /**
     * Used to calculate the amount of time it takes to pass messages between two addresses in a simulation.
     * @param source source address
     * @param destination destination address
     * @param message message being processed
     * @return amount of time that this message took in the simulation
     * @throws NullPointerException if any argument is {@code null}
     */
    Duration calculateDuration(String source, String destination, Object message);
}
