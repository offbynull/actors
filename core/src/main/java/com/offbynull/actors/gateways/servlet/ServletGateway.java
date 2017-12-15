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

import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_SERVLET;
import com.offbynull.actors.gateway.Gateway;
import com.offbynull.actors.gateways.servlet.stores.memory.MemoryStore;
import com.offbynull.actors.shuttle.Shuttle;
import com.offbynull.actors.shuttles.pump.PumpShuttle;
import com.offbynull.actors.shuttles.pump.PumpShuttleController;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import javax.servlet.http.HttpServlet;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that allows HTTP clients to send and receive messages via a servlet. To get the servlet such that you can add it to your
 * container, use {@link #getServlet() }.
 * <p>
 * HTTP requests and responses are structured as JSON objects. If you're confused about why the offset fields are required, they're for
 * guarding against inconsistencies introduced by connectivity issues. Please read the {@link Store} documentation for more information.
 * <p>
 * Requests have the following fields...
 * <ul>
 * <li>{@code id} -- Identifies the address of the HTTP client sending the message. For example, if the client were to set {@code id} to
 * {@code client_a1} and the prefix for this gateway was set to {@code servlet}, the source address of the messages being put into the
 * system must start with {@code servlet:client_a1}.</li>
 * <li>{@code outQueueOffset} -- Offset of the next message to be received by {@code id}. This servlet will remove messages before this
 * offset. For example, if the HTTP client for {@code id} wants messages from offset 15, it's assumed that the client successfully received
 * and processed messages 0 to 14 (all messages before 15).</li>
 * <li>{@code inQueueOffset} -- Offset that messages being sent by {@code id} (the messages in {@code inQueue}) should be inserted.</li>
 * <li>{@code inQueue} -- Array of messages coming from {@code id}.</li>
 * </ul>
 * <p>
 * Responses have the following fields...
 * <ul>
 * <li>{@code outQueue} -- Array of messages going to {@code id}, starting from the offset in {@code outQueueOffset}</li>
 * </ul>
 * <p>
 * The {@code inQueue}/{@code outQueue} field is a JSON array of JSON objects with the following fields...
 * <ul>
 * <li>{@code source} -- Source address of the message.</li>
 * <li>{@code destination} -- Destination address of the message.</li>
 * <li>{@code type} -- Message payload's class type.</li>
 * <li>{@code data} -- Message payload.</li>
 * </ul>
 * <p>
 * <b>IMPORTANT NOTE</b>: Concurrent HTTP calls for the same {@code id} must be avoided. This system is designed such that calls for the
 * same {@code id} must be happening serially -- clients use the result of previous calls to calculate offsets for the next call.
 * <p>
 * The following is an example request...
 * <pre>
 * {
 *   id: '0782d5a941fc97cabf18',
 *   outQueueOffset: 1,
 *   inQueueOffset: 2,
 *   inQueue: [
 *     {
 *       source: 'servlet:0782d5a941fc97cabf18',
 *       destination: 'actor:worker123:querier',
 *       type: 'java.lang.String',
 *       data: 'work_id'
 *     },
 *     {
 *       source: 'servlet:0782d5a941fc97cabf18:subsystem1',
 *       destination: 'actor:worker555',
 *       type: 'com.mycompany.messages.NewWork',
 *       data: {
 *        id: 'work_id',
 *        timeout: 1000
 *       }
 *     }
 *   ]
 * }
 * </pre>
 * The following is an example response...
 * <pre>
 * {
 *   outQueue: [
 *     {
 *       source: 'actor:worker789:querier',
 *       destination: 'servlet:0782d5a941fc97cabf18:subsystem1',
 *       type: 'java.lang.String',
 *       data: 'WORK_DONE'
 *     }
 *   ]
 * }
 * </pre>
 * @author Kasra Faghihi
 */
public final class ServletGateway implements Gateway {

    private final Shuttle inShuttle;
    private final PumpShuttleController pumpShuttleController;
    
    private final ConcurrentHashMap<String, Shuttle> outShuttles;
    
    private final CountDownLatch shutdownLatch;

    private final MessageBridgeServlet servlet;

    /**
     * Create a {@link ServletGateway} instance. Equivalent to calling
     * {@code create(CommonAddresses.DEFAULT_SERVLET, Runtime.getRuntime().availableProcessors())}.
     * @return new direct gateway
     */
    public static ServletGateway create() {
        return create(DEFAULT_SERVLET, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create a {@link ServletGateway} instance. Equivalent to calling {@code create(prefix, Runtime.getRuntime().availableProcessors())}.
     * @param prefix address prefix for this gateway
     * @throws NullPointerException if any argument is {@code null}
     * @return new direct gateway
     */
    public static ServletGateway create(String prefix) {
        return create(prefix, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create a {@link ServletGateway} instance. Equivalent to calling
     * {@code create(DEFAULT_SERVLET, MemoryStore.create(prefix, concurrency, Duration.ofSeconds(60L)))}.
     * @param prefix address prefix for this gateway
     * @param concurrency concurrency level (should be set to number of cores or larger)
     * @return new direct gateway
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if concurrency is {@code concurrency <= 0}
     */
    public static ServletGateway create(String prefix, int concurrency) {
        Validate.notNull(prefix);
        Validate.isTrue(concurrency > 0);
        return create(DEFAULT_SERVLET, MemoryStore.create(prefix, concurrency, Duration.ofSeconds(60L)));
    }

    /**
     * Create a {@link ServletGateway} instance.
     * @param prefix address prefix for this gateway
     * @param store storage engine
     * @return new servlet gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public static ServletGateway create(String prefix, Store store) {
        Validate.notNull(prefix);
        Validate.notNull(store);
        return new ServletGateway(prefix, store);
    }
    
    private ServletGateway(String prefix, Store store) {
        Validate.notNull(prefix);
        Validate.notNull(store);

        shutdownLatch = new CountDownLatch(1);
        
        inShuttle = new ServletShuttle(prefix, store, shutdownLatch);
        pumpShuttleController = PumpShuttle.create(inShuttle); // safe -- internal thread won't be started if create throws exception
        
        outShuttles = new ConcurrentHashMap<>();

        servlet = new MessageBridgeServlet(prefix, store, outShuttles, shutdownLatch);
    }
    
    @Override
    public Shuttle getIncomingShuttle() {
        if (shutdownLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        return pumpShuttleController.getPumpShuttle();
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        if (shutdownLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        String shuttlePrefix = shuttle.getPrefix();
        outShuttles.put(shuttlePrefix, shuttle);
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        if (shutdownLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        outShuttles.remove(shuttlePrefix);
    }

    /**
     * Get the servlet used to bridge web clients with the rest of the system.
     * @return message bridge servlet
     * @throws IllegalStateException if this gateway is closed
     */
    public HttpServlet getServlet() {
        if (shutdownLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        return servlet;
    }

    @Override
    public void close() {
        outShuttles.clear();
        pumpShuttleController.close();
        shutdownLatch.countDown();
    }

    @Override
    public void join() throws InterruptedException {
        pumpShuttleController.join();
        shutdownLatch.await();
    }
    
}
