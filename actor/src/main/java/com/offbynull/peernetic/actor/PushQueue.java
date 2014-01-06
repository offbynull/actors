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
import java.util.Map;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.lang3.Validate;

/**
 * Collection of outgoing messages to be populated by the user.
 * <p/>
 * Queues up outgoing messages for an {@link Actor} to send out during its step and stop sequence.
 * @author Kasra Faghihi
 */
public final class PushQueue {
    private MultiMap<Endpoint, Outgoing> outgoingMap;
    
    private TimeoutManager<Object> requestTimeoutManager;
    private Map<Object, IncomingRequest> requestIdMap;
    
    private IdCounter idCounter;

    PushQueue(IdCounter idCounter, TimeoutManager<Object> requestTimeoutManager, Map<Object, IncomingRequest> requestIdMap,
            MultiMap<Endpoint, Outgoing> outgoingMap) {
        Validate.notNull(outgoingMap);
        Validate.notNull(requestIdMap);
        Validate.notNull(requestTimeoutManager);
        
        this.outgoingMap = outgoingMap;
        this.requestTimeoutManager = requestTimeoutManager;
        this.requestIdMap = requestIdMap;
        this.idCounter = idCounter;
    }
    
    /**
     * Send a message that doesn't expect a response.
     * @param destination destination
     * @param content content
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void push(Endpoint destination, Object content) {
        Validate.notNull(destination);
        Validate.notNull(content);
        outgoingMap.put(destination, new OutgoingRequest(null, destination, content));
    }

    /**
     * Send a message that does expect a response.
     * @param destination destination
     * @param content content
     * @param maxTimestamp maximum amount of time to wait for a response (if exceeded, response won't be accepted even if it arrives)
     * @throws NullPointerException if any arguments are {@code null}
     */    
    public void pushRequest(Endpoint destination, Object content, long maxTimestamp) {
        Validate.notNull(destination);
        Validate.notNull(content);
        outgoingMap.put(destination, new OutgoingRequest(idCounter.getNext(), destination, content));
    }

    /**
     * Send a response to a message.
     * @param request message being responded to
     * @param destination destination
     * @param content content
     * @return {@code true} if response was queued, {@code false} if message was already responded to or doesn't expect a response.
     * @throws NullPointerException if any arguments are {@code null}
     */    
    public boolean pushResponse(IncomingRequest request, Endpoint destination, Object content) {
        Validate.notNull(request);
        Validate.notNull(destination);
        Validate.notNull(content);
        Object requestId = request.getId();
        
        if (requestId == null) {
            return false;
        }
        
        if (requestTimeoutManager.cancel(requestId)) {
            requestIdMap.remove(requestId);
            outgoingMap.put(destination, new OutgoingRequest(null, destination, content));
            
            return true;
        }
        
        return false;
    }
}
