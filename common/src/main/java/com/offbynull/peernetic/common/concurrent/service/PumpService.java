/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.common.concurrent.service;

import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.util.Iterator;

/**
 * {@link PumpService} is an abstract class that should be extended by any class whose instances are to be executed in their own thread and
 * is expected to receive messages from a {@link PumpReader}.
 * <p/>
 * Very similar to {@link Service}, except that this class has {@link #onStep(long, java.util.Iterator) } instead of
 * {@link Service#onProcess() }. The difference being that {@link #onStep(long, java.util.Iterator) } gets called when the pump has
 * something to feed it or enough time has elapsed.
 * @param <T> message type
 * @author Kasra Faghihi
 */
public abstract class PumpService<T> extends Service {

    private PumpReader<T> pumpReader;
    
    /**
     * Constructs a {@link Service} object.
     * @param name thread name
     * @param daemon thread daemon
     * @throws NullPointerException if any argument is {@code null}
     */
    public PumpService(String name, boolean daemon) {
        super(name, daemon);
    }

    /**
     * Constructs a {@link Service} object with a custom stop trigger.
     * @param name thread name
     * @param daemon thread daemon
     * @param stopTrigger stop trigger
     * @throws NullPointerException if any argument is {@code null}
     */
    public PumpService(String name, boolean daemon, ServiceStopTrigger stopTrigger) {
        super(name, daemon, stopTrigger);
    }

    @Override
    protected final void onProcess() throws Exception {
        long waitUntil = Long.MAX_VALUE;
        
        while (true) {
            Iterator<T> messages = pumpReader.pull(waitUntil);

            long preStepTime = System.currentTimeMillis();
            long nextStepTime = onStep(preStepTime, messages);
            
            if (nextStepTime < 0L) {
                return;
            }
            
            long postStepTime = System.currentTimeMillis();
            
            waitUntil = Math.max(nextStepTime - postStepTime, 0L);
        }
    }
    
    /**
     * Called when messages are available or the timeout duration has elapsed.
     * @param timestamp current timestamp
     * @param messages messages
     * @return maximum amount of time to wait until next trigger, or a negative value to shutdown the service
     */
    protected abstract long onStep(long timestamp, Iterator<T> messages);
    
}
