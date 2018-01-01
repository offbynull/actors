/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.gateways.direct;

import com.offbynull.actors.address.Address;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import com.offbynull.actors.shuttles.simple.Bus;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DirectShuttle implements Shuttle {

    private static final Logger LOG = LoggerFactory.getLogger(DirectShuttle.class);
    
    private final String prefix;
    private final ConcurrentHashMap<Address, Bus> readQueues;

    DirectShuttle(String prefix, ConcurrentHashMap<Address, Bus> readQueues) {
        Validate.notNull(prefix);
        Validate.notNull(readQueues);
        // don't null check outShuttle keys/values -- this is a concurrent map that gets modified elsewhere, so we have no control

        this.prefix = prefix;
        this.readQueues = readQueues;
    }
    

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        messages.stream().forEach(m -> {
            Address dst = m.getDestinationAddress();
            try {
                String dstPrefix = dst.getElement(0);
                Validate.isTrue(dstPrefix.equals(prefix));
            } catch (Exception e) {
                LOG.error("Error shuttling message: " + m, e);
            }
                
            Address next = dst;
            while (true) {
                Bus queue = readQueues.get(next);
                if (queue != null) {
                    queue.add(m);
                }
                if (next.size() == 1) {
                    break;
                }
                next = next.removeSuffix(1);
            }
        });
    }
    
}
