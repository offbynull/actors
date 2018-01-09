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
package com.offbynull.actors.gateways.threadpool;

import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.apache.commons.collections4.map.UnmodifiableMap;
import static org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap;
import org.apache.commons.lang3.Validate;

final class ThreadPoolShuttle implements Shuttle {
    private final String prefix;
    private final ExecutorService threadPool;
    private final ConcurrentHashMap<String, Shuttle> outShuttles;
    private final UnmodifiableMap<Class<?>, ThreadPoolProcessor> payloadTypes;

    ThreadPoolShuttle(
            String prefix,
            ExecutorService threadPool,
            Map<Class<?>, ThreadPoolProcessor> payloadTypes,
            ConcurrentHashMap<String, Shuttle> outShuttles) {
        Validate.notNull(prefix);
        Validate.notNull(threadPool);
        Validate.notNull(payloadTypes);
        Validate.notNull(outShuttles); // outShuttles is concurrent map -- entries added/removed on the fly, don't nullcheck keys or values
        Validate.noNullElements(payloadTypes.keySet());
        Validate.noNullElements(payloadTypes.values());
        this.prefix = prefix;
        this.threadPool = threadPool;
        this.payloadTypes = (UnmodifiableMap<Class<?>, ThreadPoolProcessor>) unmodifiableMap(new HashMap<>(payloadTypes));
        this.outShuttles = outShuttles;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        if (threadPool.isShutdown()) {
            return;
        }

        messages.stream()
                .filter(m -> m.getDestinationAddress().size() <= 2)
                .filter(m -> m.getDestinationAddress().getElement(0).equals(prefix))
                .forEach(m -> {
                    ThreadPoolProcessor processor = payloadTypes.get(m.getMessage().getClass());
                    if (processor != null) {
                        Callable<Object> callable = () -> {
                            processor.process(m, outShuttles);
                            return null;
                        };
                        threadPool.submit(callable);
                    }
                });
    }
}
