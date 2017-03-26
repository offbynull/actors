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
package com.offbynull.actors.core.actor;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttles.simple.Bus;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ActorThread {
    private static final Logger LOG = LoggerFactory.getLogger(ActorThread.class);
    
    private final Thread thread;
    private final ActorRunnable runnable;
    private final Bus bus;

    private ActorThread(Thread thread, Bus bus, ActorRunnable runnable) {
        Validate.notNull(thread);
        Validate.notNull(bus);
        Validate.notNull(runnable);
        this.thread = thread;
        this.bus = bus;
        this.runnable = runnable;
    }
    
    // it should be fine to have this be a constructor since the this pointer never gets passed to the runnable, but have this factory
    // method anyway...
    public static ActorThread create(String prefix, Shuttle selfShuttle, Runnable criticalFailureHandler) {
        Validate.notNull(prefix);
        Validate.notNull(selfShuttle);
        Validate.notNull(criticalFailureHandler);
        
        // create runnable
        Bus bus = new Bus();
        ActorRunnable runnable = new ActorRunnable(prefix, bus, criticalFailureHandler);

        // add in our own shuttle as well so we can send msgs to ourselves
        bus.add(new AddShuttle(selfShuttle));

        // start thread
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(ActorRunnable.class.getSimpleName());
        thread.start();

        // return
        return new ActorThread(thread, bus, runnable);
    }

    // singals close, but doesn't wait for the the thread to die... use join for that
    public void close() {
        try {
            bus.close();
        } catch (RuntimeException e) {
            LOG.error("Error closing bus", e);
        }
        
        try {
            thread.interrupt();
        } catch (RuntimeException e) {
            LOG.error("Error interrupting thread", e);
        }
    }

    public void join() throws InterruptedException {
        thread.join();
    }
    
    public Shuttle getIncomingShuttle() {
        return runnable.getIncomingShuttle();
    }

    public void addActor(String id, Coroutine coroutine, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        runnable.addActor(id, coroutine, primingMessages);
    }

    public void removeActor(String id) {
        Validate.notNull(id);
        runnable.removeActor(id);
    }

    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        runnable.addOutgoingShuttle(shuttle);
    }

    public void removeOutgoingShuttle(String prefix) {
        Validate.notNull(prefix);
        runnable.removeOutgoingShuttle(prefix);
    }
    
}
