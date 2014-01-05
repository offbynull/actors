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
package com.offbynull.peernetic.actor.endpoints.local;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointHandler;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.MessageUtils;
import com.offbynull.peernetic.actor.Outgoing;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.Validate;

public final class LocalEndpointHandler implements EndpointHandler<LocalEndpoint> {

    @Override
    public void push(Endpoint source, LocalEndpoint destination, Collection<Outgoing> outgoing) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.noNullElements(outgoing);
        
        Collection<Incoming> incoming = new ArrayList<>(outgoing.size());
        
        for (Outgoing outgoingMsg : outgoing) {
            Incoming incomingMsg = MessageUtils.flip(source, outgoingMsg);
            incoming.add(incomingMsg);
        }
        
        destination.getActorQueueWriter().push(incoming);
    }
    
}
