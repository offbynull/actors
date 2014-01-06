/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.helpers;

/**
 * Notifies when a certain time is reached. Intended to be called periodically by an {@link Actor}.
 * @author Kasra Faghihi
 */
public final class NotifyManager {
    private long hitTimestamp = Long.MAX_VALUE;
    
    /**
     * Check to see if the current time has been reached or exceeded the hit time.
     * @param timestamp current time
     * @return {@code true} if the time being tracked has been reached or exceeded, {@code false} otherwise
     */
    public boolean process(long timestamp) {
        return timestamp >= hitTimestamp;
    }
    
    /**
     * Resets the hit time for this notification.
     * @param nextTimestamp new hit time
     */
    public void reset(long nextTimestamp) {
        hitTimestamp = nextTimestamp;
    }

    /**
     * Get the hit time.
     * @return hit time
     */
    public long getNextTimeoutTimestamp() {
        return hitTimestamp;
    }
}
