package com.offbynull.peernetic;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import org.apache.commons.lang3.Validate;

public final class SessionManager<A> {
    private final Map<A, Slot<A>> sessionLookup;
    private final PriorityQueue<Slot<A>> sessionTimeoutQueue;
    
    private Instant lastCallTime;

    public SessionManager() {
        sessionLookup = new HashMap<>();
        sessionTimeoutQueue = new PriorityQueue<>(new SlotTimeoutComparator<A>());
    }
    
    public void addSession(Instant time, Duration duration, A id, Object param) {
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
                
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        Validate.notNull(id);

        prune(time);
        lastCallTime = time;
        
        Validate.isTrue(sessionLookup.get(id) == null, "Session already exists");
        
        Instant pruneTime = time.plus(duration);
        Slot<A> session = new Slot<>(pruneTime, id, param);

        
        sessionLookup.put(id, session);
        sessionTimeoutQueue.add(session);
    }

    public void removeSession(Instant time, A id) {
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        Validate.notNull(id);

        prune(time);
        lastCallTime = time;
        
        Slot<A> session = sessionLookup.remove(id);
        Validate.isTrue(session != null);
        session.ignore(); // equivalent to sessionTimeoutQueue.remove(session);, will be removed when encountered
    }

    public void addOrUpdateSession(Instant time, Duration duration, A id, Object param) {
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        Validate.notNull(id);
        
        prune(time);
        lastCallTime = time;

        Validate.isTrue(sessionLookup.get(id) != null, "Session does not exist");
        
        if (containsSession(time, id)) {
            removeSession(time, id);
        }
        addSession(time, duration, id, param);
    }

    public List<A> getSessions(Instant time) {
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        
        prune(time);
        lastCallTime = time;
        
        return new ArrayList<>(sessionLookup.keySet());
    }

    public Object getSessionParam(Instant time, A id) {
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        Validate.notNull(id);
        
        prune(time);
        lastCallTime = time;
        
        Slot<A> session = sessionLookup.get(id);
        
        Validate.isTrue(session != null);
        
        return session.getParam();
    }
    
    public boolean containsSession(Instant time, A source) {
        Validate.isTrue(lastCallTime == null ? true : !lastCallTime.isAfter(time));
        Validate.notNull(source);
        return sessionLookup.containsKey(source);
    }

    public int size() {
        return sessionLookup.size();
    }
    
    public void prune(Instant time) {
        Iterator<Slot<A>> it = sessionTimeoutQueue.iterator();
        while (it.hasNext()) {
            Slot<A> next = it.next();
            
            if (next.isIgnore()) {
                it.remove();
                continue;
            }
            
            if (next.getPruneTime().isAfter(time)) {
                break;
            }
            
            it.remove();
            
            sessionLookup.remove(next.getAddress());
        }
    }
    
    private static final class Slot<A> {
        private final Instant pruneTime;
        private final A source;
        private final Object param;
        private boolean ignore;

        public Slot(Instant pruneTime, A source, Object param) {
            this.pruneTime = pruneTime;
            this.source = source;
            this.param = param;
            this.ignore = false;
        }

        public Instant getPruneTime() {
            return pruneTime;
        }

        public A getAddress() {
            return source;
        }

        public Object getParam() {
            return param;
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
            final Slot<?> other = (Slot<?>) obj;
            if (!Objects.equals(this.pruneTime, other.pruneTime)) {
                return false;
            }
            if (!Objects.equals(this.source, other.source)) {
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
