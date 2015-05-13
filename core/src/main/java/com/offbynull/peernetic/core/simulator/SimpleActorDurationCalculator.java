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

import java.time.Duration;
import org.apache.commons.lang3.Validate;

/**
 * A {@link ActorDurationCalculator} that always returns a {@code 0} duration.
 * @author Kasra Faghihi
 */
public final class SimpleActorDurationCalculator implements ActorDurationCalculator {

    @Override
    public Duration calculateDuration(String source, String destination, Object message, Duration realDuration) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.notNull(realDuration);
        Validate.isTrue(!realDuration.isNegative());
        return Duration.ZERO;
    }
    
}
