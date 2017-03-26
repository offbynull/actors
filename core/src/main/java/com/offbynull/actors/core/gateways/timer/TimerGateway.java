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
package com.offbynull.actors.core.gateways.timer;

import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_TIMER;
import com.offbynull.actors.core.gateway.Gateway;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.gateway.InputGateway;
import com.offbynull.actors.core.gateway.OutputGateway;
import com.offbynull.actors.core.shuttles.simple.Bus;
import com.offbynull.actors.core.shuttles.simple.SimpleShuttle;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that accepts a message and echoes them back after a certain duration of time.
 * <p>
 * In the following example, the actor called {@code tester} sends a message to the {@link TimerGateway} called {@code timer} and requests
 * that message be echo'd back to it after 2000 milliseconds.
 * <pre>
 * Coroutine tester = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 * 
 *     // Normally, actors shouldn't be logging to System.out or doing any other IO. They're logging to System.out here for simplicity. If 
 *     // you need to do logging in your actor, use LogGateway instead.
 *     String timerPrefix = ctx.getIncomingMessage();
 *     ctx.addOutgoingMessage(Address.fromString("fromid"), Address.fromString(timerPrefix + ":2000:extra"), 0);
 *     System.out.println("ADDED TRIGGER FOR FOR 2 SECOND");
 *     cnt.suspend();
 *     System.out.println("TRIGGERED FROM " + ctx.getSource()+ " TO " + ctx.getDestination()+ " WITH " + ctx.getIncomingMessage());
 * };
 * 
 * TimerGateway timerGateway = new TimerGateway("timer");
 * Shuttle timerInputShuttle = timerGateway.getIncomingShuttle();
 * 
 * ActorRunner testerRunner = ActorRunner.create("local");
 * Shuttle testerInputShuttle = testerRunner.getIncomingShuttle();
 * 
 * testerRunner.addOutgoingShuttle(timerInputShuttle);
 * timerGateway.addOutgoingShuttle(testerInputShuttle);
 * 
 * testerRunner.addActor("tester", tester, "timer");
 * </pre>
 * @author Kasra Faghihi
 */
public final class TimerGateway implements InputGateway, OutputGateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;
    
    /**
     * Create a {@link TimerGateway} instance. Equivalent to calling {@code create(DefaultAddresses.DEFAULT_TIMER)}.
     * @return new direct gateway
     */
    public static TimerGateway create() {
        return create(DEFAULT_TIMER);
    }

    /**
     * Create a {@link TimerGateway} instance.
     * @param prefix address prefix for this gateway
     * @return new direct gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public static TimerGateway create(String prefix) {
        TimerGateway gateway = new TimerGateway(prefix);
        gateway.thread.start();
        return gateway;
    }
    
    private TimerGateway(String prefix) {
        Validate.notNull(prefix);

        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        thread = new Thread(new TimerRunnable(bus));
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
        bus.add(new AddShuttle(shuttle));
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        bus.add(new RemoveShuttle(shuttlePrefix));
    }

    @Override
    public void close() throws InterruptedException {
        thread.interrupt();
        thread.join();
    }
}
