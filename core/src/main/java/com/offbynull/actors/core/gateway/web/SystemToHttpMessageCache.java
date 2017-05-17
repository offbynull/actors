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
package com.offbynull.actors.core.gateway.web;

import com.offbynull.actors.core.shuttle.Message;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.Validate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

final class SystemToHttpMessageCache {
    private final String prefix;
    private final CacheMap<String, Message> cacheMap;

    public SystemToHttpMessageCache(String prefix, long timeout) {
        Validate.notNull(prefix);
        Validate.isTrue(timeout > 0L);
        
        cacheMap = new CacheMap<>(timeout);

        this.prefix = prefix;
    }

    public void add(long time, List<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        messages.stream().forEach(message -> {
            Validate.isTrue(message.getDestinationAddress().size() > 0);
            Validate.isTrue(message.getDestinationAddress().getElement(0).equals(prefix));
            
        });
        Validate.isTrue(time >= 0L);
        
        messages.forEach(message -> {
            String id = message.getDestinationAddress().getElement(1);
            cacheMap.add(time, id, Collections.singleton(message));
        });
    }

    public void filter(long time, String id, long maxSeq) {
        Validate.notNull(id);
        Validate.isTrue(time >= 0L);
        
        cacheMap.acknowledge(time, id, maxSeq);
    }
    
    public SortedMap<Long, Message> read(long time, String id) {
        Validate.notNull(id);
        Validate.isTrue(time >= 0L);
        
        return cacheMap.read(time, id);
    }
    
    private static final class CacheMap<K, V> {
        private final Map<K, ValueHolder> data;
        private final BidiMap<K, KeyState> states;
        private final PriorityQueue<KeyState> timeoutQueue;
        private final long timeoutDuration;

        public CacheMap(long timeoutDuration) {
            Validate.isTrue(timeoutDuration > 0L);

            this.data = new HashMap<>();
            this.states = new DualHashBidiMap<>();
            this.timeoutQueue = new PriorityQueue<>();
            this.timeoutDuration = timeoutDuration;
        }
        
        public void add(long time, K key, Collection<V> values) {
            Validate.notNull(key);
            Validate.notNull(values);
            Validate.noNullElements(values);
            Validate.isTrue(time >= 0L);
            Validate.isTrue(values.size() > 0);

            clear(time);
            // update(time, key); // do not update last access time, the system is trying to add stuff to the outgoing message queue

            ValueHolder valueHolder = data.get(key);
            if (valueHolder != null) {
                for (Object value : values) {
                    valueHolder.add(value);
                }
            }
        }
        
        public void acknowledge(long time, K key, long maxSeq) {
            Validate.notNull(key);
            Validate.isTrue(time >= 0L);

            clear(time);
            update(time, key); // update last access time -- when this method is invoked it's probably because the client requested it

            ValueHolder valueHolder = data.get(key);
            if (valueHolder != null) {
                valueHolder.acknowledge(maxSeq);
            }
        }
        
        public SortedMap<Long, V> read(long time, K key) {
            Validate.notNull(key);
            Validate.isTrue(time >= 0L);

            clear(time);
            update(time, key); // update last access time -- when this method is invoked it's probably because the client requested it

            ValueHolder valueHolder = data.get(key);
            return valueHolder == null ? Collections.emptySortedMap() : valueHolder.get();
        }
        
        private void update(long time, K key) {
            KeyState newKState = new KeyState(time);
            KeyState oldKState = states.put(key, newKState);
            timeoutQueue.add(newKState);
            if (oldKState != null) {
                oldKState.ignore = true;
            }
            
            data.computeIfAbsent(key, x -> new ValueHolder());
        }

        private void clear(long time) {
            KeyState entry;
            while ((entry = timeoutQueue.peek()) != null) {
                if (entry.ignore) {
                    timeoutQueue.poll();
                    continue;
                }
                
                long diff = time - entry.lastAccessTime;
                if (diff >= timeoutDuration) {
                    timeoutQueue.poll();
                    K key = states.removeValue(entry);
                    if (key != null) {
                        data.remove(key);
                    }
                    continue;
                }
                
                break;
            }
        }
    }
    
    private static final class KeyState implements Comparator<KeyState> {
        private final long lastAccessTime;
        private boolean ignore;

        public KeyState(long lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }

        @Override
        public int compare(KeyState o1, KeyState o2) {
            return Long.compare(o1.lastAccessTime, o2.lastAccessTime);
        }
    }
    
    private static final class ValueHolder<V> {
        private long nextSeq;
        private final TreeMap<Long, V> data;
        
        public ValueHolder() {
            nextSeq = 0;
            data = new TreeMap<>();
        }
        
        public void add(V item) {
            Validate.notNull(item);
            
            data.put(nextSeq, item);
            nextSeq++;
        }
        
        public void acknowledge(long maxSeq) {
            NavigableMap<Long, V> discardData = data.headMap(maxSeq, true);
            if (!discardData.isEmpty() && discardData.lastKey().equals(maxSeq)) {
                discardData.clear();
            }
        }
        
        public SortedMap<Long, V> get() {
            return new TreeMap<>(data);
        }
    }
}
