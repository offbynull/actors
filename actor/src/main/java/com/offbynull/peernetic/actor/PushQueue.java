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

import java.util.Collection;
import java.util.Map;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

/**
 * Collection of outgoing messages to be populated by the user.
 * <p/>
 * Queues up outgoing messages for an {@link Actor} to send out during its step and stop sequence.
 * @author Kasra Faghihi
 */
public final class PushQueue {
    private MultiMap<Endpoint, Outgoing> outgoingMap;

    PushQueue() {
        this.outgoingMap = new MultiValueMap<>();
    }
    
    /**
     * Send a message.
     * @param destination destination
     * @param content content
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void push(Endpoint destination, Object content) {
        Validate.notNull(destination);
        Validate.notNull(content);
        outgoingMap.put(destination, new Outgoing(content, destination));
    }
    
    void flush(Endpoint source) {
        for (Map.Entry<Endpoint, Object> entry : outgoingMap.entrySet()) {
            Endpoint dstEndpoint = entry.getKey();
            Collection<Outgoing> outgoing = (Collection<Outgoing>) entry.getValue();

            dstEndpoint.push(source, outgoing);
        }
        
        outgoingMap.clear();
    }
}
