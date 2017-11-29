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
package com.offbynull.actors.gateway;

import com.offbynull.actors.shuttle.Address;

/**
 * Common gateway addresses.
 * @author Kasra Faghihi
 */
public final class CommonAddresses {
    /**
     * Default address to actor runner as String.
     */
    public static final String DEFAULT_ACTOR = "actor";
    
    /**
     * Default address to actor runner.
     */
    public static final Address DEFAULT_RUNNER_ADDRESS = Address.of(DEFAULT_ACTOR);

    /**
     * Default address to direct gateway as String.
     */
    public static final String DEFAULT_DIRECT = "direct";

    /**
     * Default address to direct gateway.
     */
    public static final Address DEFAULT_DIRECT_ADDRESS = Address.of(DEFAULT_DIRECT);

    /**
     * Default address to log gateway as String.
     */
    public static final String DEFAULT_LOG = "log";

    /**
     * Default address to log gateway.
     */
    public static final Address DEFAULT_LOG_ADDRESS = Address.of(DEFAULT_LOG);

    /**
     * Default address to timer gateway as String.
     */
    public static final String DEFAULT_TIMER = "timer";

    /**
     * Default address to timer gateway.
     */
    public static final Address DEFAULT_TIMER_ADDRESS = Address.of(DEFAULT_TIMER);

    private CommonAddresses() {
        // do nothing
    }

}
