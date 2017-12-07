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
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls a {@link PumpShuttle}.
 * @author Kasra Faghihi
 */
public final class PumpShuttleController implements Closeable {
    
    private static final Logger LOG = LoggerFactory.getLogger(PumpShuttleController.class);

    private final LinkedBlockingQueue<Collection<Message>> queue;    
    private final PumpShuttle pumpShuttle;
    private final Shuttle backingShuttle;
    private final PumpRunnable runnable;
    private final Thread thread;
    private final int warnThreshold;
    private volatile boolean closed;

    PumpShuttleController(PumpShuttle pumpShuttle, Shuttle backingShuttle, LinkedBlockingQueue<Collection<Message>> queue,
            int warnThreshold) {
        Validate.notNull(pumpShuttle);
        Validate.notNull(backingShuttle);
        Validate.notNull(queue);
        Validate.isTrue(warnThreshold >= 0);

        this.queue = queue;
        this.pumpShuttle = pumpShuttle;
        this.backingShuttle = backingShuttle;
        this.warnThreshold = warnThreshold;
        this.runnable = new PumpRunnable();
        this.thread = new Thread(runnable);
        this.thread.setName("PumpShuttleThread - " + pumpShuttle.getPrefix());
        this.thread.setDaemon(true);
    }

    void start() {
        thread.start();
    }

    /**
     * Get shuttle for this controller.
     * @return pump shuttle
     */
    public PumpShuttle getPumpShuttle() {
        return pumpShuttle;
    }

    @Override
    public void close() {
        closed = true;
        thread.interrupt();
    }

    /**
     * Wait for the {@link PumpShuttle} thread to die.
     * @throws InterruptedException if interrupted
     */
    public void join() throws InterruptedException {
        thread.join();
    }



    private final class PumpRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    Collection<Message> messages = queue.take();
                    backingShuttle.send(messages);
                    
                    int queueSize = queue.size();
                    if (queueSize >= warnThreshold) {
                        LOG.warn("Queue size exceeds warning threshold: {} messages for {}", queueSize, backingShuttle.getPrefix());
                    }
                }
            } catch (InterruptedException ie) {
                // clear interrupted flag
                Thread.interrupted();

                // flush remaining
                List<Collection<Message>> finalMessages = new ArrayList<>();
                queue.drainTo(finalMessages);
                finalMessages.forEach(messages -> backingShuttle.send(messages));

                // return if closed, or throw exception if actually interrupted
                if (closed) {
                    return;
                }

                throw new IllegalStateException(ie);
            }
        }
    }
}
