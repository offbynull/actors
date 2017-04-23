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
package com.offbynull.actors.core.gateways.log;

import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_LOG;
import com.offbynull.actors.core.gateway.Gateway;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttles.simple.Bus;
import com.offbynull.actors.core.shuttles.simple.SimpleShuttle;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that logs by piping messages to SLF4J.
 * <p>
 * In the following example, the actor called {@code tester} sends an info log message to the {@link LogGateway} called {@code logger}.
 * <pre>
 * Coroutine tester = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 * 
 *     String loggerAddress = ctx.getIncomingMessage();
 *     ctx.addOutgoingMessage(loggerAddress, LogMessage.info("This is an info msg: {} {}", "test arg 1", 2));
 *     cnt.suspend();
 * };
 * 
 * LogGateway logger = LogGateway.create("logger");
 * Shuttle logInputShuttle = logGateway.getIncomingShuttle();
 * 
 * ActorRunner testerRunner = ActorRunner.create("local");
 * Shuttle testerInputShuttle = testerRunner.getIncomingShuttle();
 * 
 * testerRunner.addOutgoingShuttle(logInputShuttle);
 * 
 * testerRunner.addActor("tester", tester, "logger");
 * </pre>
 * @author Kasra Faghihi
 */
public final class LogGateway implements Gateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;

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
        thread = new Thread(new LogRunnable(bus));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        // does nothing
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        // does nothing
    }

    @Override
    public void close() throws InterruptedException {
        thread.interrupt();
        thread.join();
    }
}