/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

/**
 * Collection of outgoing messages to be populated by the user.
 * <p/>
 * Queues up outgoing messages for an {@link Actor} to send out during its step and stop sequence.
 * @author Kasra Faghihi
 */
public final class PushQueue {
    private Map<Endpoint, MultiValueMap<Endpoint, Outgoing>> outgoingMap; // src -> map<dest, list<msg>>

    PushQueue() {
        this.outgoingMap = new HashMap<>();
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
        
        MultiValueMap<Endpoint, Outgoing> msgsFromSrc = outgoingMap.get(null);
        if (msgsFromSrc == null) {
            msgsFromSrc = new MultiValueMap<>();
            outgoingMap.put(null, msgsFromSrc);
        }
        
        msgsFromSrc.put(destination, new Outgoing(content, destination));
    }

    /**
     * Send a message masquerading as if it was sent from some other {@link Endpoint}. Useful if you're proxying endpoints.
     * @param reportedSource source to put in as the sender
     * @param destination destination
     * @param content content
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void push(Endpoint reportedSource, Endpoint destination, Object content) {
        Validate.notNull(reportedSource);
        Validate.notNull(destination);
        Validate.notNull(content);
        
        MultiValueMap<Endpoint, Outgoing> msgsFromSrc = outgoingMap.get(reportedSource);
        if (msgsFromSrc == null) {
            msgsFromSrc = new MultiValueMap<>();
            outgoingMap.put(reportedSource, msgsFromSrc);
        }
        
        msgsFromSrc.put(destination, new Outgoing(content, destination));
    }

    void drain(Collection<Outgoing> dst) {
        Validate.notNull(dst);
        
        for (Map.Entry<Endpoint, MultiValueMap<Endpoint, Outgoing>> entry : outgoingMap.entrySet()) {
            for (Map.Entry<Endpoint, Object> innerEntry : entry.getValue().entrySet()) {
                Collection<Outgoing> outgoing = (Collection<Outgoing>) innerEntry.getValue();
                dst.addAll(outgoing);
            }
        }
        
        outgoingMap.clear();
    }

    void flush(Endpoint defaultSource) {
        Validate.notNull(defaultSource);
        
        for (Map.Entry<Endpoint, MultiValueMap<Endpoint, Outgoing>> entry : outgoingMap.entrySet()) {
            Endpoint srcEndpoint = entry.getKey();
            if (srcEndpoint == null) {
                srcEndpoint = defaultSource;
            }
            
            for (Map.Entry<Endpoint, Object> innerEntry : entry.getValue().entrySet()) {
                Endpoint dstEndpoint = innerEntry.getKey();
                
                Collection<Outgoing> outgoing = (Collection<Outgoing>) innerEntry.getValue();
                dstEndpoint.push(srcEndpoint, outgoing);
            }
        }
        
        outgoingMap.clear();
    }
}
