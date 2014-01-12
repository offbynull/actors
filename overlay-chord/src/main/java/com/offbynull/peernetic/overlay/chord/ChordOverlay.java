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
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.security.SecureRandom;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * A chord overlay implementation.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class ChordOverlay<A> extends Actor {
    private Pointer<A> self;
    private Pointer<A> bootstrap;
    private EndpointFinder<A> finder;
    private SecureRandom secureRandom;
    
    private ChordOverlayListener<A> listener;
    private ChordTask<A> chordTask;

    /**
     * Construct a {@link ChordOverlay} object.
     * @param self id and address of this node
     * @param bootstrap id and address of the bootstrap node (can be {@code null})
     * @param finder finder
     * @param listener listener
     * @throws NullPointerException if any argument other than {@code bootstrap} is {@code null}
     * @throws IllegalArgumentException if {@code self} and {@code bootstrap} don't share the same limit or have limits that aren't
     * {@code 2^n-1}
     */
    public ChordOverlay(Pointer<A> self, Pointer<A> bootstrap, EndpointFinder<A> finder, ChordOverlayListener<A> listener) {
        Validate.notNull(self);
        Validate.notNull(finder);
        Validate.notNull(listener);
        if (bootstrap != null) {
            Validate.isTrue(self.getId().getLimitAsBigInteger().equals(bootstrap.getId().getLimitAsBigInteger()));
        }
        IdUtils.ensureLimitPowerOfTwo(self);
        
        this.self = self;
        this.bootstrap = bootstrap;
        this.finder = finder;
        this.listener = listener;
    }
    
    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {        
        secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
        chordTask = new ChordTask<>(self, bootstrap, secureRandom, finder, listener);

        
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
