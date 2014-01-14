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
package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Outgoing;
import com.offbynull.peernetic.actor.TemporaryEndpoint;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * Access functionality of a Chord node.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class ChordAccessor<A> {
    private Endpoint endpoint;
    
    /**
     * Construct a {@link ChordAccessor} object.
     * @param endpoint endpoint of chord node
     */
    public ChordAccessor(Endpoint endpoint) {
        Validate.notNull(endpoint);
        
        this.endpoint = endpoint;
    }
    
    /**
     * Ask node to route to a certain ID.
     * @param id id
     * @param timeout timeout
     * @param unit timeout unit
     * @return pointer of closest node to ID, or {@code null} if could not be found / timedout
     * @throws InterruptedException if thread interrupted
     */
    public Pointer<A> routeTo(Id id, long timeout, TimeUnit unit) throws InterruptedException {
        TemporaryEndpoint self = new TemporaryEndpoint();
        
        Outgoing outgoing = new Outgoing(new RouteRequest(id), endpoint);
        endpoint.push(self, outgoing);
        
        List<Object> result = self.poll(timeout, unit);
        
        if (result == null || result.isEmpty()) {
            return null;
        }
        
        return ((RouteResponse<A>) result.get(0)).getDestination();
    }
}
