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
package com.offbynull.actors.gateways.timer;

import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_TIMER;
import com.offbynull.actors.gateway.Gateway;
import com.offbynull.actors.shuttle.Shuttle;
import com.offbynull.actors.shuttles.simple.Bus;
import com.offbynull.actors.shuttles.simple.SimpleShuttle;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that accepts a message and echoes them back after a certain duration of time. To specify the duration when a message is
 * echoed back, append it to the destination address. For example, if this gateway has the prefix {@code "timer"} and you want it to echo
 * a message back after 2000 milliseconds, send that message to {@code "timer:2000"}.
 * @author Kasra Faghihi
 */
public final class TimerGateway implements Gateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;
    
    private final AtomicBoolean shutdownFlag;
    
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
        shutdownFlag = new AtomicBoolean(false);
        thread = new Thread(new TimerRunnable(bus, shutdownFlag));
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
        Validate.notNull(shuttle);
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }
        
        bus.add(new AddShuttle(shuttle));
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        if (shutdownFlag.get()) {
            throw new IllegalStateException();
        }
        
        bus.add(new RemoveShuttle(shuttlePrefix));
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
