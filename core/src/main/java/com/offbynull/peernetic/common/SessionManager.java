package com.offbynull.peernetic.common;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class SessionManager<A> implements Processable {
    private final StepTimer<A> timer;
    private final Map<A, Object> values;

    public SessionManager() {
        timer = new StepTimer<>();
        values = new HashMap<>();
    }
    
    public void addSession(Instant time, Duration duration, A id, Object param) {
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
        Validate.notNull(id);
        Validate.notNull(time);

        Validate.isTrue(!timer.contains(id), "Id already exists");
        
        timer.add(time, duration, id);
        values.put(id, param);
    }

    public void addOrUpdateSession(Instant time, Duration duration, A id, Object param) {
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
        Validate.notNull(id);
        Validate.notNull(time);
        
        if (containsSession(id)) {
            removeSession(id);
        }
        addSession(time, duration, id, param);
    }
    
    public void refreshSession(Instant time, Duration duration, A id) {
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
        Validate.notNull(id);
        Validate.notNull(time);
        Validate.isTrue(timer.contains(id));
        
        Object param = values.get(id);

        removeSession(id);
        addSession(time, duration, id, param);
    }

    public void removeSession(A id) {
        Validate.notNull(id);
        
        timer.cancel(id);
        values.remove(id);
    }

    public boolean containsSession(A id) {
        Validate.notNull(id);
        
        return timer.contains(id);
    }

    public Set<A> getSessions() {
        return timer.getKeys();
    }

    public Object getSessionParam(A id) {
        Validate.notNull(id);
                
        return values.get(id);
    }

    public void assignSessionParam(A id, Object value) {
        Validate.notNull(id);
        Validate.isTrue(timer.contains(id), "Id does not exist");
        
        values.put(id, value);
    }
    
    public int size() {
        return timer.size();
    }

    @Override
    public Duration process(Instant time) {
        Duration ret = timer.process(time);
        timer.getRemovedKeys().forEach(x -> values.remove(x));
        return ret;
    }

    public Set<A> getRemovedIds() {
        return timer.getRemovedKeys();
    }
}
