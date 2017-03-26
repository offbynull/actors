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
package com.offbynull.actors.core.gateways.direct;

import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttles.simple.Bus;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DirectRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DirectRunnable.class);

    private final Map<String, Shuttle> outgoingShuttles;
    private final LinkedBlockingQueue<Message> readQueue;
    private final Bus bus;

    public DirectRunnable(Bus bus, LinkedBlockingQueue<Message> readQueue) {
        Validate.notNull(bus);
        Validate.notNull(readQueue);
        Validate.isTrue(readQueue.isEmpty()); // just in case
        outgoingShuttles = new HashMap<>();
        this.readQueue = readQueue;
        this.bus = bus;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Poll for new messages
                List<Object> incomingObjects = bus.pull();

                Validate.notNull(incomingObjects);
                Validate.noNullElements(incomingObjects);

                // Queue new messages
                for (Object incomingObj : incomingObjects) {
                    if (incomingObj instanceof Message) {
                        Message message = (Message) incomingObj;
                        readQueue.add(message);
                    } else {
                        LOG.debug("Processing management message: {}", incomingObj);
                        if (incomingObj instanceof AddShuttle) {
                            AddShuttle addShuttle = (AddShuttle) incomingObj;
                            Shuttle shuttle = addShuttle.getShuttle();
                            Shuttle existingShuttle = outgoingShuttles.putIfAbsent(shuttle.getPrefix(), shuttle);
                            Validate.validState(existingShuttle == null);
                        } else if (incomingObj instanceof RemoveShuttle) {
                            RemoveShuttle removeShuttle = (RemoveShuttle) incomingObj;
                            String prefix = removeShuttle.getPrefix();
                            Shuttle oldShuttle = outgoingShuttles.remove(prefix);
                            Validate.validState(oldShuttle != null);
                        } else if (incomingObj instanceof SendMessages) {
                            SendMessages sendMessage = (SendMessages) incomingObj;
                            for (Message message : sendMessage.getMessages()) {
                                String dstPrefix = message.getDestinationAddress().getElement(0);
                                Shuttle shuttle = outgoingShuttles.get(dstPrefix);

                                if (shuttle != null) {
                                    shuttle.send(Collections.singleton(message));
                                } else {
                                    LOG.warn("Unable to find shuttle for outgoing message: {}", message);
                                }
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Direct gateway interrupted");
            Thread.interrupted();
        } catch (RuntimeException re) {
            LOG.error("Internal error encountered", re);
        } finally {
            bus.close();
        }
    }

}
