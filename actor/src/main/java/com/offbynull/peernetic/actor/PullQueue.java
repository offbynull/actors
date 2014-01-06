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

/**
 * Collection of incoming messages.
 * <p/>
 * Used for processing incoming messages for each step of an {@link Actor}.
 * @author Kasra Faghihi
 */
public final class PullQueue {
    private TimeoutManager<RequestKey> outgoingRequestTimeoutManager;
    private Iterator<Incoming> requestPointer;
    private Iterator<Incoming> responsePointer;

    PullQueue(TimeoutManager<RequestKey> outgoingRequestTimeoutManager,
            Collection<Incoming> incoming) {
        Validate.notNull(outgoingRequestTimeoutManager);
        Validate.noNullElements(incoming);
        
        this.outgoingRequestTimeoutManager = outgoingRequestTimeoutManager;
        this.requestPointer = incoming.iterator();
        this.responsePointer = incoming.iterator();
    }

    /**
     * Get the next incoming request.
     * @return next incoming request, or {@code null} if non exists
     */
    public IncomingRequest pullRequest() {
        while (requestPointer.hasNext()) {
            Incoming incoming = requestPointer.next();
            
            if (incoming instanceof IncomingRequest) {
                IncomingRequest request = (IncomingRequest) incoming;
                return request;
            }
        }
        
        return null;
    }

    /**
     * Get the next incoming response.
     * @return next incoming response, or {@code null} if non exists
     */
    public IncomingResponse pullResponse() {
        while (responsePointer.hasNext()) {
            Incoming incoming = responsePointer.next();
            
            if (incoming instanceof IncomingResponse) {
                IncomingResponse response = (IncomingResponse) incoming;
                
                Object id = response.getId();
                Endpoint source = response.getSource();
                RequestKey key = new RequestKey(source, id);
                
                boolean canceled = outgoingRequestTimeoutManager.cancel(key);
                if (canceled) {
                    return (IncomingResponse) response;
                }
            }
        }
        
        return null;
    }
}
