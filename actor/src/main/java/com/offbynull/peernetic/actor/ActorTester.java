/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Use to test an {@link Actor} without actually starting it. Feed in predetermined timestamps and messages to see if the expected outputs
 * occur.
 * @author Kasra Faghihi
 */
public final class ActorTester {
    private Actor actor;
    private Map<Object, Object> initMap;
    
    /**
     * Constructs a {@link ActorTester} object.
     * @param actor actor to test -- consumed upon creation
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if {@code actor} already consumed
     */
    public ActorTester(Actor actor) {
        Validate.notNull(actor);
        
        this.actor = actor;
        initMap = actor.consume();
    }
    
    /**
     * Invoke wrapping actor's {@link Actor#onStart(long, com.offbynull.peernetic.actor.PushQueue, java.util.Map) }.
     * @param timestamp timestamp to pass in
     * @return outgoing messages produced by the actor
     * @throws IllegalStateException if invocation throws an exception
     */
    public Collection<Outgoing> start(long timestamp) {
        PushQueue pushQueue = new PushQueue();
        try {
            actor.invokeOnStart(timestamp, pushQueue, initMap);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
        final List<Outgoing> dst = new ArrayList<>();
        pushQueue.drain(dst);
                
        return dst;
    }

    /**
     * Invoke wrapping actor's {@link Actor#onStep(long, com.offbynull.peernetic.actor.PullQueue, com.offbynull.peernetic.actor.PushQueue,
     * com.offbynull.peernetic.actor.Endpoint) }.
     * @param timestamp timestamp to pass in
     * @param incoming incoming messages to pass in
     * @return outgoing messages produced by the actor
     * @throws NullPointerException if {@code incoming} contains {@code null}
     * @throws IllegalStateException if invocation throws an exception
     */
    public Collection<Outgoing> step(long timestamp, Incoming ... incoming) {
        Validate.noNullElements(incoming);
        
        PullQueue pullQueue = new PullQueue(Arrays.asList(incoming));
        PushQueue pushQueue = new PushQueue();
        try {
            actor.invokeOnStep(timestamp, pullQueue, pushQueue, new NullEndpoint());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
        final List<Outgoing> dst = new ArrayList<>();
        pushQueue.drain(dst);
                
        return dst;
    }

    /**
     * Invoke wrapping actor's {@link Actor#onStop(long, com.offbynull.peernetic.actor.PushQueue) }.
     * @param timestamp timestamp to pass in
     * @return outgoing messages produced by the actor
     * @throws IllegalStateException if invocation throws an exception
     */
    public Collection<Outgoing> stop(long timestamp) {
        PushQueue pushQueue = new PushQueue();
        try {
            actor.invokeOnStop(timestamp, pushQueue);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
        final List<Outgoing> dst = new ArrayList<>();
        pushQueue.drain(dst);
                
        return dst;
    }
}
