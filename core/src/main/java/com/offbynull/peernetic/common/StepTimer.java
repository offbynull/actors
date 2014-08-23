package com.offbynull.peernetic.common;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class StepTimer<K> implements Processable {
    private final Map<K, Slot<K>> lookup;
    private final PriorityQueue<Slot<K>> timeoutQueue;
    private final Set<K> removedKeys; // keys removed from last process call
    
    private Instant lastCallTime;

    public StepTimer() {
        lookup = new HashMap<>();
        timeoutQueue = new PriorityQueue<>(new SlotTimeoutComparator<K>());
        removedKeys = new HashSet<>();
    }
    
    public void add(Instant time, Duration duration, K key) {
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
        Validate.notNull(key);

        Validate.isTrue(lookup.get(key) == null, "Key already exists");

        lastCallTime = time;
        
        Instant pruneTime = time.plus(duration);
        Slot<K> slot = new Slot<>(pruneTime, key);

        
        lookup.put(key, slot);
        timeoutQueue.add(slot);
    }

    public void cancel(K key) {
        Validate.notNull(key);
        
        Slot<K> slot = lookup.remove(key);
        Validate.isTrue(key != null);
        
        slot.ignore(); // equivalent to nonceTimeoutQueue.cancel(nonce);, will be removed when encountered
    }

    public boolean contains(K key) {
        Validate.notNull(key);
        return lookup.containsKey(key);
    }
    
    public int size() {
        return lookup.size();
    }
    
    @Override
    public Duration process(Instant time) {
        removedKeys.clear();

        Iterator<Slot<K>> it = timeoutQueue.iterator();
        while (it.hasNext()) {
            Slot<K> next = it.next();
            
            if (next.isIgnore()) {
                it.remove();
                continue;
            }
            
            if (next.getPruneTime().isAfter(time)) {
                break;
            }
            
            it.remove();
            
            lookup.remove(next.getKey());
            removedKeys.add(next.getKey());
        }
        
        return timeoutQueue.isEmpty() ? null : Duration.between(time, timeoutQueue.peek().getPruneTime());
    }
    
    public Set<K> getKeys() {
        return new HashSet<>(lookup.keySet());
    }
    
    public Set<K> getRemovedKeys() {
        return new HashSet<>(removedKeys);
    }
    
    private static final class Slot<K> {
        private final Instant pruneTime;
        private final K key;
        private boolean ignore;

        public Slot(Instant pruneTime, K key) {
            this.pruneTime = pruneTime;
            this.key = key;
            this.ignore = false;
        }

        public Instant getPruneTime() {
            return pruneTime;
        }

        public K getKey() {
            return key;
        }

        public boolean isIgnore() {
            return ignore;
        }

        public void ignore() {
            this.ignore = true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.pruneTime);
            hash = 67 * hash + Objects.hashCode(this.key);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Slot<?> other = (Slot<?>) obj;
            if (!Objects.equals(this.pruneTime, other.pruneTime)) {
                return false;
            }
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }

    }
    private static final class SlotTimeoutComparator<A> implements Comparator<Slot<A>>, Serializable {

        private static final long serialVersionUID = 0L;
        
        @Override
        public int compare(Slot<A> o1, Slot<A> o2) {
            return o1.getPruneTime().compareTo(o2.getPruneTime());
        }
        
    }
}
