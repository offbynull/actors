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
package com.offbynull.peernetic.common.concurrent.actor.helpers;

import com.offbynull.peernetic.common.concurrent.actor.Actor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * Manages timeouts for a set of resources. Intended to be called periodically by an {@link Actor}.
 * @author Kasra Faghihi
 * @param <R> resource
 */
public final class TimeoutManager<R> {
    private Map<R, Entity> keyMap = new HashMap<>();
    private LinkedList<Entity> idQueue = new LinkedList<>();
    
    /**
     * Add a timeout for a resource.
     * @param res resource
     * @param maxTimestamp timestamp for which the resource expires@
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void add(R res, long maxTimestamp) {
        Validate.notNull(res);
        
        Validate.isTrue(!keyMap.containsKey(res));
        
        Entity entity = new Entity(maxTimestamp, res);
        
        keyMap.put(res, entity);
        idQueue.addLast(entity);
    }

    /**
     * Cancel the timeout for a resource.
     * @param key resource
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void cancel(R key) {
        Validate.notNull(key);
        
        Entity entity = keyMap.remove(key);
        if (entity != null) {
            entity.cancel();
        }
    }

    /**
     * Cancel the timeout for all resources that were added.
     * @return all resources that were being tracked
     */
    public TimeoutManagerResult<R> flush() {
        Set<R> pending = new HashSet<>();
        
        for (Entity entity : idQueue) {
            if (entity.isCancelled()) {
                continue;
            }
            pending.add(entity.getKey());
        }
        
        idQueue.clear();
        pending.clear();
        
        return new TimeoutManagerResult(pending, Long.MAX_VALUE);
    }
    
    /**
     * Get expired resources.
     * @param timestamp current timestamp
     * @return expired resources
     */
    public TimeoutManagerResult<R> process(long timestamp) {
        Set<R> timedOut = new HashSet<>();
        long waitDuration = 0L;
        
        while (true) {
            Entity entity = idQueue.peekFirst();
            
            if (entity == null) {
                break;
            }
            
            if (entity.isCancelled()) {
                idQueue.poll();
                continue;
            }
            
            if (timestamp >= entity.getMaxTimestamp()) {
                timedOut.add(entity.getKey());
                idQueue.pollFirst();
                keyMap.remove(entity.getKey());
            } else {
                waitDuration = entity.getMaxTimestamp() - timestamp;
                if (waitDuration <= 0L) {
                    waitDuration = 1L;
                }
                break;
            }
        }
        
        return new TimeoutManagerResult<>(timedOut, waitDuration);
    }
    
    /**
     * Timeout manager results.
     * @param <R> resource
     */
    public static final class TimeoutManagerResult<R> {
        private Set<R> responses;
        private long maxTimestamp;

        private TimeoutManagerResult(Set<R> responses, long maxTimestamp) {
            this.responses = Collections.unmodifiableSet(responses);
            this.maxTimestamp = maxTimestamp;
        }

        /**
         * Get expired resources.
         * @return expired resource
         */
        public Set<R> getTimedout() {
            return responses;
        }

        /**
         * Get the next timestamp a resource will expire.
         * @return next timestamp a resource will expire
         */
        public long getNextTimeoutTimestamp() {
            return maxTimestamp;
        }
        
    }
    
    private final class Entity {
        private long maxTimestamp;
        private R key;
        private boolean cancelled;

        private Entity(long maxTimestamp, R key) {
            this.maxTimestamp = maxTimestamp;
            this.key = key;
        }

        public long getMaxTimestamp() {
            return maxTimestamp;
        }

        public R getKey() {
            return key;
        }
        
        public void cancel() {
            cancelled = true;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
    }
}