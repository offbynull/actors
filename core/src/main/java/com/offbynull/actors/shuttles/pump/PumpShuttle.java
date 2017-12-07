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
package com.offbynull.actors.shuttles.pump;

import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

/**
 * A shuttle implementation that queues up and pumps messages for a potentially slower shuttle.
 * @author Kasra Faghihi
 */
public final class PumpShuttle implements Shuttle {
    private final Shuttle backingShuttle;
    private final LinkedBlockingQueue<Collection<Message>> queue;

    /**
     * Create a {@link PumpShuttle}. Equivalent to calling {@code create(backingShuttle, 1000)}.
     * @param backingShuttle shuttle being pumped to
     * @return pump shuttle controller
     * @throws NullPointerException if any argument is {@code null}
     */
    public static PumpShuttleController create(Shuttle backingShuttle) {
        return create(backingShuttle, 1000);
    }

    /**
     * Create a {@link PumpShuttle}. This method returns a controller -- use {@link PumpShuttleController#getPumpShuttle() } to get the
     * actual pump shuttle.
     * @param backingShuttle shuttle being pumped to
     * @param warnThreshold maximum queue size before logging a warning
     * @return pump shuttle controller
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code warnThreshold < 0}
     */
    public static PumpShuttleController create(Shuttle backingShuttle, int warnThreshold) {
        Validate.notNull(backingShuttle);
        Validate.isTrue(warnThreshold >= 0);

        LinkedBlockingQueue<Collection<Message>> queue = new LinkedBlockingQueue<>();

        PumpShuttle pumpShuttle = new PumpShuttle(backingShuttle, queue);
        PumpShuttleController controller = new PumpShuttleController(pumpShuttle, backingShuttle, queue, warnThreshold);

        try {
            controller.start();
        } catch (RuntimeException re) {
            controller.close();
            throw re;
        }

        return controller;
    }

    PumpShuttle(Shuttle backingShuttle, LinkedBlockingQueue<Collection<Message>> queue) {
        Validate.notNull(backingShuttle);
        Validate.notNull(queue);

        this.backingShuttle = backingShuttle;
        this.queue = queue;
    }

    @Override
    public String getPrefix() {
        return backingShuttle.getPrefix();
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        if (messages.isEmpty()) {
            return;
        }

        queue.add(messages);
    }
}
