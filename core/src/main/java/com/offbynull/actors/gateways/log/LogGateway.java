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
package com.offbynull.actors.gateways.log;

import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_LOG;
import com.offbynull.actors.gateway.Gateway;
import com.offbynull.actors.shuttle.Shuttle;
import com.offbynull.actors.shuttles.simple.Bus;
import com.offbynull.actors.shuttles.simple.SimpleShuttle;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that logs by piping messages to SLF4J. Note that this gateway...
 * <ol>
 * <li>only consumes messages (doesn't send messages).</li>
 * <li>only accepts messages of type {@link LogMessage}.</li>
 * </ol>
 * @author Kasra Faghihi
 */
public final class LogGateway implements Gateway {


    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;
    
    private final AtomicBoolean shutdownFlag;

    /**
     * Create a {@link LogGateway} instance. Equivalent to calling {@code create(DefaultAddresses.DEFAULT_LOG)}.
     * @return new direct gateway
     */
    public static LogGateway create() {
        return create(DEFAULT_LOG);
    }

    /**
     * Create a {@link LogGateway} instance.
     * @param prefix address prefix for this gateway
     * @return new direct gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public static LogGateway create(String prefix) {
        LogGateway gateway = new LogGateway(prefix);
        gateway.thread.start();
        return gateway;
    }

    private LogGateway(String prefix) {
        Validate.notNull(prefix);

        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        shutdownFlag = new AtomicBoolean(false);
        thread = new Thread(new LogRunnable(bus, shutdownFlag));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
    }

    @Override
    public Shuttle getIncomingShuttle() {
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }
        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }
        Validate.notNull(shuttle);
        // does nothing
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }
        Validate.notNull(shuttlePrefix);
        // does nothing
    }
    
    @Override
    public void close() {
        shutdownFlag.set(true);
        bus.close();
    }

    @Override
    public void join() throws InterruptedException {
        thread.join();
    }
}