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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class TimeoutManager<K> {
    private Map<K, Entity> keyMap = new HashMap<>();
    private LinkedList<Entity> idQueue = new LinkedList<>();
    
    public void add(K key, long timeoutTimestamp) {
        Validate.notNull(key);
        
        Validate.isTrue(!keyMap.containsKey(key));
        
        Entity entity = new Entity(timeoutTimestamp, key);
        
        keyMap.put(key, entity);
        idQueue.addLast(entity);
    }

    public void cancel(K key) {
        Validate.notNull(key);
        
        keyMap.remove(key).cancel();
    }

    public TimeoutManagerResult<K> flush() {
        Set<K> pending = new HashSet<>();
        
        for (Entity entity : idQueue) {
            if (entity.isCancelled()) {
                continue;
            }
            pending.add(entity.getKey());
        }
        
        idQueue.clear();
        pending.clear();
        
        return new TimeoutManagerResult(pending, 0L);
    }
    
    public TimeoutManagerResult<K> process(long currentTime) {
        Set<K> timedOut = new HashSet<>();
        long waitDuration = 0L;
        
        while (true) {
            Entity entity = idQueue.peekFirst();
            
            if (entity == null) {
                break;
            }
            
            if (entity.isCancelled()) {
                continue;
            }
            
            if (currentTime >= entity.getTimeoutTimestamp()) {
                timedOut.add(entity.getKey());
                idQueue.pollFirst();
                keyMap.remove(entity.getKey());
            } else {
                waitDuration = entity.getTimeoutTimestamp() - currentTime;
                if (waitDuration <= 0L) {
                    waitDuration = 1L;
                }
                break;
            }
        }
        
        return new TimeoutManagerResult<>(timedOut, waitDuration);
    }
    
    public static final class TimeoutManagerResult<K> {
        private Set<K> responses;
        private long waitDuration;

        private TimeoutManagerResult(Set<K> responses, long waitDuration) {
            this.responses = Collections.unmodifiableSet(responses);
            this.waitDuration = waitDuration;
        }

        public Set<K> getTimedout() {
            return responses;
        }

        public long getNextTimeoutTimestamp() {
            return waitDuration;
        }
        
    }
    
    private final class Entity {
        private long timeoutTimestamp;
        private K key;
        private boolean cancelled;

        private Entity(long timeoutTimestamp, K key) {
            this.timeoutTimestamp = timeoutTimestamp;
            this.key = key;
        }

        public long getTimeoutTimestamp() {
            return timeoutTimestamp;
        }

        public K getKey() {
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