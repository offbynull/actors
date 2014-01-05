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
 * Queue of messages to be sent out.
 * <p/>
 * Messages get queued up in each {@link Actor#onStep(long, java.util.Iterator) } invocation and are sent out once the invocation completes.
 * @author Kasra Faghihi
 */
public final class PushQueue {
    private MultiMap<Endpoint, Outgoing> outgoingMap;
    
    private TimeoutManager<Object> requestTimeoutManager;
    private Map<Object, IncomingRequest> requestIdMap;
    
    private IdCounter idCounter;

    PushQueue(IdCounter idCounter, TimeoutManager<Object> responseTimeoutManager, MultiMap<Endpoint, Outgoing> outgoingMap) {
        Validate.notNull(outgoingMap);
        Validate.notNull(responseTimeoutManager);
        
        this.outgoingMap = outgoingMap;
        this.requestTimeoutManager = responseTimeoutManager;
        this.idCounter = idCounter;
    }
    
    public void push(Endpoint destination, Object content) {
        outgoingMap.put(destination, new OutgoingRequest(null, destination, content));
    }
    
    public void pushRequest(Endpoint destination, Object content, long maxTimestamp) {
        outgoingMap.put(destination, new OutgoingRequest(idCounter.getNext(), destination, content));
    }

    public boolean pushResponse(IncomingRequest request, Endpoint destination, Object content) {
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
