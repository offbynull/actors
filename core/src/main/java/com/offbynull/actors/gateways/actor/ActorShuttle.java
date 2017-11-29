/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.gateways.actor;

import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.offbynull.actors.store.Store;

final class ActorShuttle implements Shuttle {
    private static final Logger LOG = LoggerFactory.getLogger(ActorShuttle.class);
    
    private final String prefix;
    private final Store store;
    private final AtomicBoolean shutdownFlag;

    ActorShuttle(String prefix, Store store, AtomicBoolean shutdownFlag) {
        Validate.notNull(prefix);
        Validate.notNull(store);
        Validate.notNull(shutdownFlag);

        this.prefix = prefix;
        this.store = store;
        this.shutdownFlag = shutdownFlag;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        if (shutdownFlag.get()) {
            return;
        }
        
        List<Message> filteredMessages = new ArrayList<>(messages.size());
        messages.stream().forEach(m -> {
            try {
                Address dst = m.getDestinationAddress();
                String dstPrefix = dst.getElement(0);
                Validate.isTrue(dstPrefix.equals(prefix));
                
                filteredMessages.add(m);
            } catch (Exception e) {
                LOG.error("Error shuttling message: " + m, e);
            }
        });

        store.store(filteredMessages);
    }
    
}
