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
package com.offbynull.actors.gateways.servlet;

import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.Collection;
import org.apache.commons.lang3.Validate;
import java.util.concurrent.CountDownLatch;
import static java.util.stream.Collectors.groupingBy;

final class ServletShuttle implements Shuttle {

    private final String prefix;
    private final Store queue;
    private final CountDownLatch shutdownLatch;

    ServletShuttle(String prefix, Store queue, CountDownLatch shutdownLatch) {
        Validate.notNull(prefix);
        Validate.notNull(queue);
        Validate.notNull(shutdownLatch);

        this.prefix = prefix;
        this.queue = queue;
        this.shutdownLatch = shutdownLatch;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        if (shutdownLatch.getCount() == 0L) {
            return;
        }

        messages.stream()
                .filter(m -> m.getDestinationAddress().size() <= 2)
                .filter(m -> m.getDestinationAddress().getElement(0).equals(prefix))
                .collect(groupingBy(x -> x.getDestinationAddress().getElement(1))).entrySet().stream()
                .forEach(e -> queue.write(e.getKey(), e.getValue()));
    }
    
}
