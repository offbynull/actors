/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.shuttles.simple;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple shuttle implementation. This shuttle writes to a {@link Bus}. Another thread can then read from that bus and process messages
 * process messages that are sent to this shuttle.
 * @author Kasra Faghihi
 */
public final class SimpleShuttle implements Shuttle {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleShuttle.class);
    
    private final String prefix;
    private final Bus bus;

    /**
     * Constructs a {@link SimpleShuttle} instance.
     * @param prefix address prefix of this shuttle
     * @param bus bus to shuttle to
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SimpleShuttle(String prefix, Bus bus) {
        Validate.notNull(prefix);
        Validate.notNull(bus);

        this.prefix = prefix;
        this.bus = bus;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        List<Message> filteredMessages = new ArrayList<>(messages.size());
        messages.stream().forEach(x -> {
            try {
                Address dst = x.getDestinationAddress();
                String dstPrefix = dst.getElement(0);
                Validate.isTrue(dstPrefix.equals(prefix));
                
                filteredMessages.add(x);
            } catch (Exception e) {
                LOG.error("Error shuttling message: " + x, e);
            }
        });
        
        LOG.debug("Shuttling {} messages", filteredMessages.size());
        bus.add(filteredMessages);
    }
}
