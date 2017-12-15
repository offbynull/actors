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
import java.io.IOException;
import java.io.Reader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import com.offbynull.actors.shuttle.Shuttle;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import static java.util.stream.Collectors.groupingBy;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MessageBridgeServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(MessageBridgeServlet.class);

    private final JsonConverter jsonConverter;

    private final String prefix;
    private final Store store;

    private final ConcurrentHashMap<String, Shuttle> outShuttles;
    private final CountDownLatch shutdownLatch;

    MessageBridgeServlet(String prefix,
            Store store,
            ConcurrentHashMap<String, Shuttle> outShuttles,
            CountDownLatch shutdownLatch) {
        Validate.notNull(prefix);
        Validate.notNull(store);
        Validate.notNull(outShuttles);
        Validate.notNull(shutdownLatch);
         // DONT CHECK outShuttles FOR NULL keys/values as there's no point -- map is concurrent, being modified by other threads

        this.jsonConverter = new JsonConverter();
        this.prefix = prefix;
        this.store = store;
        this.outShuttles = outShuttles;
        this.shutdownLatch = shutdownLatch;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (shutdownLatch.getCount() == 0L) {
            resp.setStatus(SC_SERVICE_UNAVAILABLE);
            return;
        }


        try {
            // Read and parse response
            String requestJson;
            try (Reader reader = req.getReader()) {
                requestJson = IOUtils.toString(reader);
            }
            RequestBlock requestBlock = jsonConverter.fromJson(requestJson);
            
            
            
            
            // Get http client's id
            String id = requestBlock.getId();

            

            
            // Check messages from the http client
            for (Message message : requestBlock.getInQueue()) {
                Validate.isTrue(message.getSourceAddress().size() >= 2);
                Validate.isTrue(message.getSourceAddress().getElement(0).equals(prefix));
                Validate.isTrue(message.getSourceAddress().getElement(1).equals(id));
            }

            // Queue (insert) messages from the http client
            store.queueIn(id, requestBlock.getInQueueOffset(), requestBlock.getInQueue());
            
            // Dequeue (remove) messages from the http client and shuttle
               // there may be no messages returned here even if there were messages inserted above -- those messages may have been dupes
            store.dequeueIn(id).stream()
                .collect(groupingBy(x -> x.getDestinationAddress().getElement(0))).entrySet().stream()
                .forEach(e -> {
                    Shuttle outShuttle = outShuttles.get(e.getKey());
                    if (outShuttle != null) {
                        outShuttle.send(e.getValue());
                    }
                });


            

            
            // Get messages to the http client starting from outQueueOffset, and dequeue (remove) everything before outQueueOffset
                // if it's asking for outQueueOffset, it means it successfully recvd everything before it, so we can dequeue it
            List<Message> outQueue = store.dequeueOut(id, requestBlock.getOutQueueOffset());

            
            
            
            
            // Create and write response
            ResponseBlock responseBlock = new ResponseBlock(outQueue);
            String responseJson = jsonConverter.toJson(responseBlock);
            try (Writer writer = resp.getWriter()) {
                IOUtils.write(responseJson, writer);
            }
        } catch (RuntimeException | IOException e) {
            resp.setStatus(SC_INTERNAL_SERVER_ERROR);
            LOG.error("Servlet failed: {}", e);
        }
    }

}
