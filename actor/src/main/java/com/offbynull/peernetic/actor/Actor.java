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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;

/**
 * {@link Actor} is an abstract class that should be extended by any class expected to implement the Actor model
 * (http://en.wikipedia.org/wiki/Actor_model).
 * @author Kasra Faghihi
 */
public abstract class Actor {
    private AtomicBoolean consumed = new AtomicBoolean();
    private Map<Object, Object> startupMap = Collections.synchronizedMap(new HashMap<>());
    
    final Map<Object, Object> consume() {
        if (consumed.getAndSet(true)) {
            throw new IllegalStateException("Already consumed");
        }
        
        Map<Object, Object> startupMapCopy = new HashMap<>(startupMap); // copy incase map is held on to, startUp map being copied
                                                                        // is synchronized -- don't want overhead as this will never
                                                                        // change once it hits starup
        startupMap.clear();
        return startupMapCopy;
    }

    /**
     * Pushes a key-value pair in to the map that gets passed in to
     * {@link #onStart(long, com.offbynull.peernetic.actor.PushQueue, java.util.Map) }.
     * @param key key -- must be immutable
     * @param value value -- must be immutable
     * @throws IllegalStateException if the actor has already been started
     * @throws NullPointerException if {@code key} is {@code null}
     */
    protected final void putInStartupMap(Object key, Object value) {
        Validate.notNull(key);
        Validate.validState(!consumed.get());

        startupMap.put(key, value);
    }
    
    final ActorStartSettings invokeOnStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        return onStart(timestamp, pushQueue, initVars);
    }

    final long invokeOnStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        return onStep(timestamp, pullQueue, pushQueue, selfEndpoint);
    }

    final void invokeOnStop(long timestamp, PushQueue pushQueue) throws Exception {
        onStop(timestamp, pushQueue);
    }
    
    /**
     * Called to initialize this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStop() }.
     * @param timestamp current timestamp
     * @param pushQueue messages to send
     * @param initVars variables to initialize this actor -- values in this map are passed in through
     * {@link #putInStartupMap(java.lang.Object, java.lang.Object) }.
     * @return start settings for this actor
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception;
    
    /**
     * Called to specify what the endpoint for this actor is right after the invoking
     * {@link #onStart(long, com.offbynull.peernetic.actor.PushQueue, java.util.Map) } by the same thread.
     * @param selfEndpoint endpoint for this actor
     */
    protected void endpointReady(Endpoint selfEndpoint) {
    }

    /**
     * Called when the internal {@link ActorQueueReader} has messages available or the maximum wait duration has elapsed. Called from
     * internally spawned thread (the same thread that called {@link #onStep(long, java.util.Iterator) } and {@link #onStop() }.
     * @param timestamp current timestamp
     * @param pullQueue messages received
     * @param pushQueue messages to send
     * @param selfEndpoint endpoint for this actor
     * @return maximum amount of time to wait until next invocation of this method, or a negative value to shutdown the service
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception;

    /**
     * Called to shutdown this actor.  Called from internally spawned thread (the same thread that called {@link #onStart() } and
     * {@link #onStep(long, java.util.Iterator) }.
     * @param timestamp current timestamp
     * @param pushQueue messages to send at the end of each invocation of this method
     * @throws Exception on error, shutdowns the internally spawned thread if encountered
     */
    protected abstract void onStop(long timestamp, PushQueue pushQueue) throws Exception;
}
