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
package com.offbynull.peernetic.core.gateways.log;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that logs by piping messages to SLF4J.
 * <p>
 * In the following example, the {@link Actor} called {@code tester} sends an info log message to the {@link LogGateway} called
 * {@code logger}.
 * <pre>
 * Coroutine tester = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 * 
 *     String loggerAddress = ctx.getIncomingMessage();
 *     ctx.addOutgoingMessage(loggerAddress, LogMessage.info("This is an info msg: {} {}", "test arg 1", 2));
 *     cnt.suspend();
 * };
 * 
 * LogGateway logger = new LogGateway("logger");
 * Shuttle logInputShuttle = logGateway.getIncomingShuttle();
 * 
 * ActorRunner testerRunner = ActorRunner.create("local");
 * Shuttle testerInputShuttle = testerRunner.getIncomingShuttle();
 * 
 * testerRunner.addOutgoingShuttle(logInputShuttle);
 * 
 * testerRunner.addCoroutineActor("tester", tester, "logger");
 * </pre>
 * @author Kasra Faghihi
 */
public final class LogGateway implements InputGateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;

    /**
     * Constructs a {@link LogGateway} instance.
     * @param prefix address prefix for this gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public LogGateway(String prefix) {
        Validate.notNull(prefix);

        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        thread = new Thread(new LogRunnable(bus));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
        thread.start();
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    @Override
    public void close() throws InterruptedException {
        thread.interrupt();
        thread.join();
    }
}