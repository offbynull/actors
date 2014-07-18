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
    private final Map<A, Session<A>> sessionLookup;
    private final PriorityQueue<Session<A>> sessionTimeoutQueue;
    
    private Instant lastCallTime;

    public SessionManager() {
        sessionLookup = new HashMap<>();
        sessionTimeoutQueue = new PriorityQueue<>(new SessionTimeoutComparator<A>());
    }
    
    public void addSession(Instant time, Duration duration, A source, Object param) {
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
                
        Validate.isTrue(lastCallTime == null ? true : lastCallTime.isBefore(time));
        Validate.notNull(source);

        Validate.isTrue(sessionLookup.get(source) == null, "Session already exists");
                
        prune(time);
        
        Instant pruneTime = time.plus(duration);
        Session<A> session = new Session<>(pruneTime, source, param);

        
        sessionLookup.put(source, session);
        sessionTimeoutQueue.add(session);
        
        lastCallTime = time;
    }

    public void removeSession(Instant time, A source) {
        Validate.isTrue(lastCallTime == null ? true : lastCallTime.isBefore(time));
        Validate.notNull(source);

        prune(time);
        
        Session<A> session = sessionLookup.remove(source);
        Validate.isTrue(session != null);
        session.ignore(); // equivalent to sessionTimeoutQueue.remove(session);, will be removed when encountered
        
        lastCallTime = time;
    }

    public void addOrUpdateSession(Instant time, Duration duration, A source, Object param) {
        Validate.isTrue(lastCallTime == null ? true : lastCallTime.isBefore(time));
        Validate.notNull(source);

        Validate.isTrue(sessionLookup.get(source) != null, "Session does not exist");
        
        if (containsSession(source)) {
            removeSession(time, source);
        }
        addSession(time, duration, source, param);
    }

    public List<A> getSessions() {
        return new ArrayList<>(sessionLookup.keySet());
    }

    public Object getSessionParam(A source) {
        Validate.notNull(source);
        Session<A> session = sessionLookup.get(source);
        
        Validate.isTrue(session != null);
        
        return session.getParam();
    }
    
    public boolean containsSession(A source) {
        Validate.notNull(source);
        return sessionLookup.containsKey(source);
    }

    public int size() {
        return sessionLookup.size();
    }
    
    public void prune(Instant time) {
        Iterator<Session<A>> it = sessionTimeoutQueue.iterator();
        while (it.hasNext()) {
            Session<A> next = it.next();
            
            if (next.isIgnore()) {
                it.remove();
                continue;
            }
            
            if (next.getPruneTime().isAfter(time)) {
                break;
            }
            
            it.remove();
            
            sessionLookup.remove(next.getSource());
        }
    }
    
    private static final class Session<A> {
        private final Instant pruneTime;
        private final A source;
        private final Object param;
        private boolean ignore;

        public Session(Instant pruneTime, A source, Object param) {
            this.pruneTime = pruneTime;
            this.source = source;
            this.param = param;
            this.ignore = false;
        }

        public Instant getPruneTime() {
            return pruneTime;
        }

        public A getSource() {
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
            final Session<?> other = (Session<?>) obj;
            if (!Objects.equals(this.pruneTime, other.pruneTime)) {
                return false;
            }
            if (!Objects.equals(this.source, other.source)) {
                return false;
            }
            return true;
        }

    }
    private static final class SessionTimeoutComparator<A> implements Comparator<Session<A>> {

        @Override
        public int compare(Session<A> o1, Session<A> o2) {
            return o1.getPruneTime().compareTo(o2.getPruneTime());
        }
        
    }
}
