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
package com.offbynull.peernetic.visualizer.gateways.graph;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.time.Instant;
import java.util.List;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GraphRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphRunnable.class);

    private final Bus bus;

    public GraphRunnable(Bus bus) {
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

                GraphApplication graph = GraphApplication.getInstance();
                if (graph == null) {
                    // TODO log warning here
                    return;
                }

                MultiMap<Address, Object> payloads = new MultiValueMap<>();
                for (Object incomingObject : incomingObjects) {
                    if (incomingObject instanceof Message) {
                        Message msg = (Message) incomingObject;

                        Address dst = msg.getDestinationAddress();
                        Object payload = msg.getMessage();
                        payloads.put(dst, payload);
                        
                        graph.execute(payloads);
                    } else if (incomingObject instanceof CreateStage) {
                        graph.execute((CreateStage) incomingObject);
                    } else {
                        throw new IllegalStateException("Unexpected message type: " + incomingObject);
                    }
                }
            }
        } catch (InterruptedException ie) {
            bus.close(); // just in case
        }
    }

    private static final class PendingMessage implements Comparable<PendingMessage> {

        private final Instant sendTime;
        private final String from;
        private final String to;
        private final Object message;

        public PendingMessage(Instant sendTime, String from, String to, Object message) {
            Validate.notNull(from);
            Validate.notNull(to);
            Validate.notNull(message);
            this.sendTime = sendTime;
            this.from = from;
            this.to = to;
            this.message = message;
        }

        public Instant getSendTime() {
            return sendTime;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public Object getMessage() {
            return message;
        }

        @Override
        public int compareTo(PendingMessage o) {
            return sendTime.compareTo(o.sendTime);
        }

    }

}
