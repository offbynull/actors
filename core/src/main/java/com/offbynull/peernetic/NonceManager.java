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

public final class NonceManager<A> {
    private final Map<A, Nonce<A>> nonceLookup;
    private final PriorityQueue<Nonce<A>> nonceTimeoutQueue;
    
    private Instant lastCallTime;

    public NonceManager() {
        nonceLookup = new HashMap<>();
        nonceTimeoutQueue = new PriorityQueue<>(new NonceTimeoutComparator<A>());
    }
    
    public void addNonce(Instant time, Duration duration, A source, Object param) {
        Validate.isTrue(lastCallTime == null ? true : lastCallTime.isBefore(time));
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
        Validate.notNull(source);

        Validate.isTrue(nonceLookup.get(source) == null, "Nonce already exists");
                
        prune(time);
        
        Instant pruneTime = time.plus(duration);
        Nonce<A> session = new Nonce<>(pruneTime, source, param);

        
        nonceLookup.put(source, session);
        nonceTimeoutQueue.add(session);
        
        lastCallTime = time;
    }

    public void removeNonce(Instant time, A source) {
        Validate.isTrue(lastCallTime == null ? true : lastCallTime.isBefore(time));
        Validate.notNull(source);

        prune(time);
        
        Nonce<A> session = nonceLookup.remove(source);
        session.ignore(); // equivalent to sessionTimeoutQueue.remove(session);, will be removed when encountered
        
        lastCallTime = time;
    }

    public Optional<Object> checkNonce(Instant time, A source) {
        Validate.isTrue(lastCallTime == null ? true : lastCallTime.isBefore(time));
        Validate.notNull(source);

        prune(time);
        
        lastCallTime = time;
                
        Nonce<A> session = nonceLookup.get(source);
        return session == null ? Optional.empty() : Optional.ofNullable(session.getResponse());
    }

    public void assignResponse(Instant time, A source, Object response) {
        Validate.isTrue(lastCallTime == null ? true : lastCallTime.isBefore(time));
        Validate.notNull(source);
        
        Nonce<A> nonce = nonceLookup.get(source);
        Validate.isTrue(nonce != null, "Nonce does not exist");
        
        nonce.setResponse(response);
    }
    
    public void prune(Instant time) {
        Iterator<Nonce<A>> it = nonceTimeoutQueue.iterator();
        while (it.hasNext()) {
            Nonce<A> next = it.next();
            
            if (next.isIgnore()) {
                it.remove();
                continue;
            }
            
            if (next.getPruneTime().isAfter(time)) {
                break;
            }
            
            it.remove();
            
            nonceLookup.remove(next.getSource());
        }
    }
    
    private static final class Nonce<A> {
        private final Instant pruneTime;
        private final A source;
        private Object response;
        private boolean ignore;

        public Nonce(Instant pruneTime, A source, Object response) {
            this.pruneTime = pruneTime;
            this.source = source;
            this.response = response;
            this.ignore = false;
        }

        public Instant getPruneTime() {
            return pruneTime;
        }

        public A getSource() {
            return source;
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
            hash = 67 * hash + Objects.hashCode(this.source);
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
            final Nonce<?> other = (Nonce<?>) obj;
            if (!Objects.equals(this.pruneTime, other.pruneTime)) {
                return false;
            }
            if (!Objects.equals(this.source, other.source)) {
                return false;
            }
            return true;
        }

    }
    private static final class NonceTimeoutComparator<A> implements Comparator<Nonce<A>> {

        @Override
        public int compare(Nonce<A> o1, Nonce<A> o2) {
            return o1.getPruneTime().compareTo(o2.getPruneTime());
        }
        
    }
}
