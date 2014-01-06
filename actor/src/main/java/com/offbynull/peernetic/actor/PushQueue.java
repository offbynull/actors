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
    
    private TimeoutManager<RequestKey> outgoingRequestTimeoutManager;
    private TimeoutManager<RequestKey> incomingRequestTimeoutManager;
    private Map<RequestKey, IncomingRequest> incomingRequestMap;
    
    private IdCounter outgoingRequestIdCounter;

    PushQueue(IdCounter outgoingRequestIdCounter,
            TimeoutManager<RequestKey> outgoingRequestTimeoutManager,
            MultiMap<Endpoint, Outgoing> outgoingMap,
            TimeoutManager<RequestKey> incomingRequestTimeoutManager,
            Map<RequestKey, IncomingRequest> incomingRequestMap) {
        Validate.notNull(outgoingMap);
        Validate.notNull(incomingRequestMap);
        Validate.notNull(outgoingRequestTimeoutManager);
        
        this.outgoingMap = outgoingMap;
        this.outgoingRequestIdCounter = outgoingRequestIdCounter;
        this.outgoingRequestTimeoutManager = outgoingRequestTimeoutManager;
        this.incomingRequestTimeoutManager = incomingRequestTimeoutManager;
        this.incomingRequestMap = incomingRequestMap;
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
        
        long id = outgoingRequestIdCounter.getNext();
        RequestKey requestKey = new RequestKey(destination, id);
        
        outgoingMap.put(destination, new OutgoingRequest(id, destination, content));
        outgoingRequestTimeoutManager.add(requestKey, 10000L);
    }

    /**
     * Send a response to a message.
     * @param request message being responded to
     * @param content content
     * @return {@code true} if response was queued, {@code false} if message was already responded to or doesn't expect a response.
     * @throws NullPointerException if any arguments are {@code null}
     */    
    public boolean pushResponse(IncomingRequest request, Object content) {
        Validate.notNull(request);
        Validate.notNull(content);
        Object requestId = request.getId();
        
        if (requestId == null || request.isResponded()) {
            return false;
        }
        
        RequestKey key = new RequestKey(request.getSource(), requestId);
        
        if (incomingRequestTimeoutManager.cancel(key)) {
            Endpoint destination = request.getSource();
            
            incomingRequestMap.remove(key);
            outgoingMap.put(destination, new OutgoingResponse(requestId, destination, content));
            request.responded();
            
            return true;
        }
        
        return false;
    }
}
