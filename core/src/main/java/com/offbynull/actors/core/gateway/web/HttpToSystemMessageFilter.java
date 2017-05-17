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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.Validate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HttpToSystemMessageFilter {
    private final String prefix;
    private final FilterMap<String> filterMap;

    public HttpToSystemMessageFilter(String prefix, long timeout) {
        Validate.notNull(prefix);
        Validate.isTrue(timeout > 0L);
        
        filterMap = new FilterMap<>(timeout);

        this.prefix = prefix;
    }

    public List<Message> filter(long time, long seq, List<Message> messages) {
        Validate.isTrue(time >= 0L);
        Validate.isTrue(seq >= 0L);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        messages.forEach(message -> {
            Validate.isTrue(message.getSourceAddress().size() > 0);
            Validate.isTrue(message.getSourceAddress().getElement(0).equals(prefix));
        });
        
        List<Message> ret = new ArrayList<>(messages.size());
        for (Message message : messages) {
            String id = message.getSourceAddress().getElement(1);
            if (filterMap.updateSequence(time, id, seq)) {
                ret.add(message);
            }
            seq++;
        }
        
        return ret;
    }

    public long latestSequence(String httpAddressId) {
        return filterMap.getSequence(httpAddressId);
    }
    
    private static final class FilterMap<K> {
        private final Map<K, ValueHolder> data;
        private final BidiMap<K, KeyState> states;
        private final PriorityQueue<KeyState> timeoutQueue;
        private final long timeoutDuration;

        public FilterMap(long timeoutDuration) {
            Validate.isTrue(timeoutDuration > 0L);

            this.data = new HashMap<>();
            this.states = new DualHashBidiMap<>();
            this.timeoutQueue = new PriorityQueue<>();
            this.timeoutDuration = timeoutDuration;
        }
        
        public long getSequence(K key) {
            Validate.notNull(key);
            return data.get(key).getMaxSequence();
        }
        
        public boolean updateSequence(long time, K key, long sequence) {
            Validate.notNull(key);
            Validate.isTrue(time >= 0L);

            clear(time);
            update(time, key);

            ValueHolder valueHolder = data.get(key);
            if (valueHolder.updateMaxSequence(sequence)) {
                update(time, key);
                return true;
            }
            
            return false;
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
        private long maxSeq;
        
        public ValueHolder() {
            maxSeq = 0;
        }
        
        public boolean updateMaxSequence(long sequence) {
            if (sequence < maxSeq) {
                return false;
            }
            
            maxSeq = sequence;
            return true;
        }

        public long getMaxSequence() {
            return maxSeq;
        }
        
    }
}
