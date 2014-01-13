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
package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * A chord overlay implementation.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class ChordOverlay<A> extends Actor {
    private ChordConfig<A> config;
    private ChordTask<A> chordTask;

    /**
     * Construct a {@link ChordOverlay} object.
     * @param config chord configuration
     * @throws NullPointerException if any argument is {@code null}
     */
    public ChordOverlay(ChordConfig<A> config) {
        Validate.notNull(config);
        config.lock();
        
        this.config = config;
    }
    
    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {        
        chordTask = new ChordTask<>(config);
        
        return new ActorStartSettings(timestamp); // hit immediately
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        long nextHitTime = Long.MAX_VALUE;
        boolean hit = false;
        
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            hit = true;
            long stepNextHitTime = chordTask.process(timestamp, incoming, pushQueue);
            nextHitTime = Math.min(nextHitTime, stepNextHitTime);
        }
        
        if (!hit) {
            nextHitTime = chordTask.process(timestamp, null, pushQueue);
        }
        
        return nextHitTime;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }
}
