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
package com.offbynull.peernetic.actor;

import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.lang3.Validate;

public final class PullQueue {
    private TimeoutManager<Object> responseTimeoutManager;
    private Iterator<Incoming> requestPointer;
    private Iterator<Incoming> responsePointer;

    PullQueue(TimeoutManager<Object> responseTimeoutManager, Collection<Incoming> incoming) {
        Validate.notNull(responseTimeoutManager);
        Validate.noNullElements(incoming);
        
        this.responseTimeoutManager = responseTimeoutManager;
        this.requestPointer = incoming.iterator();
        this.responsePointer = incoming.iterator();
    }

    public IncomingRequest pullRequest() {
        while (requestPointer.hasNext()) {
            Incoming incoming = requestPointer.next();
            
            if (incoming instanceof IncomingRequest) {
                return (IncomingRequest) incoming;
            }
        }
        
        return null;
    }
    
    public IncomingResponse pullResponse() {
        while (responsePointer.hasNext()) {
            Incoming incoming = responsePointer.next();
            
            if (incoming instanceof IncomingResponse) {
                IncomingResponse response = (IncomingResponse) incoming;
                
                Object id = response.getId();
                boolean canceled = responseTimeoutManager.cancel(id);
                
                if (canceled) {
                    return (IncomingResponse) response;
                }
            }
        }
        
        return null;
    }
}
