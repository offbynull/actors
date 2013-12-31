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
package com.offbynull.peernetic.common.concurrent.pump;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.util.Iterator;
import org.apache.commons.lang3.Validate;

/**
 * {@link Actor} is an abstract class that should be extended by any class expected to receive messages from a {@link PumpReader} in
 * an isolated internal thread. See {@link #onStep(long, java.util.Iterator) }.
 * @param <T> message type
 * @author Kasra Faghihi
 */
public abstract class Actor<T> extends AbstractExecutionThreadService {

    private PumpReader<T> pumpReader;
    
    /**
     * Constructs a {@link Actor} object.
     * @param name thread name
     * @param daemon thread daemon
     * @param pumpReader pump reader
     * @throws NullPointerException if any argument is {@code null}
     */
    public Actor(String name, boolean daemon, PumpReader<T> pumpReader) {
        super(name, daemon);
        Validate.notNull(pumpReader);
        
        this.pumpReader = pumpReader;
    }

    /**
     * Constructs a {@link Actor} object with a custom stop trigger.
     * @param name thread name
     * @param daemon thread daemon
     * @param pumpReader pump reader
     * @throws NullPointerException if any argument is {@code null}
     */
    public Actor(String name, boolean daemon, PumpReader<T> pumpReader) {
        super(name, daemon);
        Validate.notNull(pumpReader);
        
        this.pumpReader = pumpReader;
    }

    @Override
    protected final void run() throws Exception {
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
     * Called when the internal {@link PumpReader} has messages available or the maximum wait duration has elapsed.
     * @param timestamp current timestamp
     * @param messages messages from the internal {@link PumpReader}
     * @return maximum amount of time to wait until next invokation of this method, or a negative value to shutdown the service
     */
    protected abstract long onStep(long timestamp, Iterator<T> messages);
}
