package com.offbynull.peernetic;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import org.apache.commons.lang3.Validate;

public final class NonceManager<T> {
    private final Map<Nonce<T>, Slot<T>> nonceLookup;
    private final PriorityQueue<Slot<T>> nonceTimeoutQueue;
    
    private Instant lastCallTime;

    public NonceManager() {
        nonceLookup = new HashMap<>();
        nonceTimeoutQueue = new PriorityQueue<>(new SlotTimeoutComparator<T>());
    }
    
    public void addNonce(Instant time, Duration duration, Nonce<T> nonce, Object response) {
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
        Validate.notNull(nonce);

        Validate.isTrue(nonceLookup.get(nonce) == null, "Nonce already exists");

        lastCallTime = time;
        
        Instant pruneTime = time.plus(duration);
        Slot<T> slot = new Slot<>(pruneTime, nonce, response);

        
        nonceLookup.put(nonce, slot);
        nonceTimeoutQueue.add(slot);
    }

    public void removeNonce(Nonce<T> nonce) {
        Validate.notNull(nonce);
        
        Slot<T> slot = nonceLookup.remove(nonce);
        Validate.isTrue(nonce != null);
        
        slot.ignore(); // equivalent to nonceTimeoutQueue.remove(nonce);, will be removed when encountered
    }

    public Optional<Object> checkNonce(Nonce<T> nonce) {
        Validate.notNull(nonce);
                
        Slot<T> slot = nonceLookup.get(nonce);
        return slot == null ? null : Optional.ofNullable(slot.getResponse());
    }

    public void assignResponse(Nonce<T> nonce, Object response) {
        Validate.notNull(nonce);
        
        Slot<T> slot = nonceLookup.get(nonce);
        Validate.isTrue(slot != null, "Nonce does not exist");
        
        slot.setResponse(response);
    }
    
    public int size() {
        return nonceLookup.size();
    }
    
    public Map<Nonce<T>, Object> prune(Instant time) {
        Map<Nonce<T>, Object> ret = new HashMap<>();
        
        Iterator<Slot<T>> it = nonceTimeoutQueue.iterator();
        while (it.hasNext()) {
            Slot<T> next = it.next();
            
            if (next.isIgnore()) {
                it.remove();
                continue;
            }
            
            if (next.getPruneTime().isAfter(time)) {
                break;
            }
            
            it.remove();
            
            nonceLookup.remove(next.getNonce());
            
            ret.put(next.getNonce(), next.getResponse());
        }
        
        return ret;
    }
    
    private static final class Slot<A> {
        private final Instant pruneTime;
        private final Nonce<A> nonce;
        private Object response;
        private boolean ignore;

        public Slot(Instant pruneTime, Nonce<A> nonce, Object response) {
            this.pruneTime = pruneTime;
            this.nonce = nonce;
            this.response = response;
            this.ignore = false;
        }

        public Instant getPruneTime() {
            return pruneTime;
        }

        public Nonce<A> getNonce() {
            return nonce;
        }

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            this.response = response;
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
            hash = 67 * hash + Objects.hashCode(this.nonce);
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
            if (!Objects.equals(this.nonce, other.nonce)) {
                return false;
            }
            return true;
        }

    }
    private static final class SlotTimeoutComparator<A> implements Comparator<Slot<A>> {

        @Override
        public int compare(Slot<A> o1, Slot<A> o2) {
            return o1.getPruneTime().compareTo(o2.getPruneTime());
        }
        
    }
}
