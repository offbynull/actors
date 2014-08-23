package com.offbynull.peernetic.common.message;

import com.offbynull.peernetic.common.Processable;
import com.offbynull.peernetic.common.StepTimer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class NonceManager<T> implements Processable {
    private final StepTimer<Nonce<T>> timer;
    private final Map<Nonce<T>, Object> values;

    public NonceManager() {
        timer = new StepTimer<>();
        values = new HashMap<>();
    }
    
    public void addNonce(Instant time, Duration duration, Nonce<T> nonce, Object value) {
        Validate.isTrue(!duration.isNegative() && !duration.isZero());
        Validate.notNull(nonce);

        Validate.isTrue(!timer.contains(nonce), "Nonce already exists");
        
        timer.add(time, duration, nonce);
        values.put(nonce, value);
    }

    public void removeNonce(Nonce<T> nonce) {
        Validate.notNull(nonce);
        
        timer.cancel(nonce);
        values.remove(nonce);
    }

    public boolean isNoncePresent(Nonce<T> nonce) {
        Validate.notNull(nonce);
                
        return values.containsKey(nonce);
    }
    
    public Object getNonceValue(Nonce<T> nonce) {
        Validate.notNull(nonce);
        Validate.isTrue(timer.contains(nonce), "Nonce does not exist");
                
        return values.get(nonce);
    }

    public void assignValue(Nonce<T> nonce, Object value) {
        Validate.notNull(nonce);
        Validate.isTrue(timer.contains(nonce), "Nonce does not exist");
        
        values.put(nonce, value);
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

    public Set<Nonce<T>> getRemovedNonces() {
        return timer.getRemovedKeys();
    }
}
