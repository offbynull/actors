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
import java.util.List;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GraphRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GraphRunnable.class);

    private final Bus bus;

    public GraphRunnable(Bus bus) {
        Validate.notNull(bus);
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
                Validate.notNull(graph, "Graph application isn't running");

                for (Object incomingObj : incomingObjects) {
                    if (incomingObj instanceof Message) {
                        MultiMap<Address, Object> payloads = new MultiValueMap<>();
                        Message msg = (Message) incomingObj;

                        Address dst = msg.getDestinationAddress();
                        Object payload = msg.getMessage();
                        payloads.put(dst, payload);
                        
                        LOG.debug("Processing incoming message from {} to {}: {}", msg.getSourceAddress(), dst, payload);
                        graph.execute(payloads);
                    } else if (incomingObj instanceof UpdateHandlers) {
                        LOG.debug("Processing update handlers message: {} ", incomingObj);
                        graph.execute((UpdateHandlers) incomingObj);
                    } else if (incomingObj instanceof CreateStage) {
                        LOG.debug("Processing create stage message: {} ", incomingObj);
                        graph.execute((CreateStage) incomingObj);
                    } else {
                        throw new IllegalStateException("Unexpected message type: " + incomingObj);
                    }
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Graph gateway interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        } finally {
            bus.close();
        }
    }

}
