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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.lang3.Validate;

/**
 * An endpoint for a {@link Actor}.
 * @author Kasra Faghihi
 */
public final class LocalEndpoint implements Endpoint {
    private final ActorQueue actorQueue; // actorqueue implementation is thread safe

    LocalEndpoint(ActorQueue actorQueue) {
        Validate.notNull(actorQueue);
        this.actorQueue = actorQueue;
    }

    @Override
    public void push(Endpoint source, Collection<Outgoing> outgoing) {
        Validate.notNull(source);
        Validate.noNullElements(outgoing);
        
        Collection<Incoming> incoming = new ArrayList<>(outgoing.size());
        
        for (Outgoing outgoingMsg : outgoing) {
            Incoming incomingMsg = MessageUtils.flip(source, outgoingMsg);
            incoming.add(incomingMsg);
        }
        
        actorQueue.getWriter().push(incoming);
    }

    @Override
    public void push(Endpoint source, Outgoing... outgoing) {
        push(source, Arrays.asList(outgoing));
    }
    
}
