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
package com.offbynull.actors.shuttles.blackhole;

import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.Collection;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A shuttle implementation that discards all messages sent to it.
 * @author Kasra Faghihi
 */
public final class BlackholeShuttle implements Shuttle {

    private static final Logger LOG = LoggerFactory.getLogger(BlackholeShuttle.class);
    private final String prefix;

    /**
     * Constructs a {@link BlackholeShuttle} instance.
     * @param prefix address prefix of this shuttle
     * @throws NullPointerException if any argument is {@code null}
     */
    public BlackholeShuttle(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
    }
    
    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        // do nothing
        
        LOG.debug("Received {} messages", messages.size());
    }
    
}
