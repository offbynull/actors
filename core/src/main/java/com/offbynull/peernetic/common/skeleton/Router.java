package com.offbynull.peernetic.common.skeleton;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.StepTimer;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.NonceGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

public final class Router<A, N> {
    private NonceAccessor<N> nonceAccessor;

    private MultiMap<Class<?>, Actor> typeHandlers;
    private Map<Nonce<N>, Actor> nonceHandlers;
    private StepTimer<Nonce<N>> nonceTimer;
    
    public Router(Endpoint selfEndpoint, NonceGenerator<N> nonceGenerator, NonceAccessor<N> nonceAccessor) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceAccessor);
        
        this.nonceAccessor = nonceAccessor;
    
        typeHandlers = new MultiValueMap<>();
        nonceHandlers = new HashMap<>();
        nonceTimer = new StepTimer<>();
    }
    
    public void addTypeHandler(Actor actor, Class<?> type) {
        Validate.notNull(actor);
        Validate.notNull(type);

        typeHandlers.put(type, actor);
    }
    
    public void removeTypeHandler(Actor actor, Class<?> type) {
        Validate.notNull(actor);
        Validate.notNull(type);
        
        typeHandlers.removeMapping(type, actor);
    }

    public void removeAllTypeHandlersForActor(Actor actor) {
        Validate.notNull(actor);
        
        Set<Class<?>> keysCopy = new HashSet<>(typeHandlers.keySet());
        keysCopy.stream().forEach(k -> typeHandlers.removeMapping(k, actor));
    }

    public void removeAllTypeHandlers(Class<?> type) {
        Validate.notNull(type);
        typeHandlers.remove(type);
    }

    public void removeActor(Actor actor) {
        Validate.notNull(actor);
        
        removeAllNoncesForActor(actor);
        removeAllTypeHandlersForActor(actor);
    }
    
    public void addNonce(Instant time, Nonce<N> nonce, Actor actor, Duration duration) {
        Validate.notNull(time);
        Validate.notNull(nonce);
        Validate.notNull(actor);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        
        Actor existingActor = nonceHandlers.putIfAbsent(nonce, actor);
        Validate.isTrue(existingActor == null, "Nonce already exists.");
        nonceTimer.add(time, duration, nonce);
    }

    public void removeNonce(Nonce<N> nonce) {
        Validate.notNull(nonce);
        nonceHandlers.remove(nonce);
        nonceTimer.cancel(nonce);
    }

    public void removeAllNoncesForActor(Actor actor) {
        Validate.notNull(actor);
        
        nonceHandlers = new HashMap<>();
        nonceTimer = new StepTimer<>();
    }
    
    public void route(Instant time, Object message, Endpoint srcEndpoint) throws Exception {
        Validate.notNull(time);
        Validate.notNull(message);
        Validate.notNull(srcEndpoint);
        
        flush(time);
        
        
        // type check
        Collection<Actor> actors = (Collection<Actor>) typeHandlers.get(message.getClass());
        actors = CollectionUtils.emptyIfNull(actors);
        for (Actor actor : actors) { // wrap in new list to avoid concurrent modification exception
            actor.onStep(time, srcEndpoint, message);
        }
        
        
        // check for responses to previous outgoing requests
        if (!nonceAccessor.containsNonceField(message)) {
            return;
        }
        
        Actor actor = nonceHandlers.remove(nonceAccessor.get(message));
        if (actor != null) {
            actor.onStep(time, srcEndpoint, message);
        }
    }
    
    public void flush(Instant time) {
        nonceTimer.process(time);
        nonceTimer.getRemovedKeys().forEach(x -> nonceHandlers.remove(x));
    }

//    @Override
//    public Duration process(Instant time) {
//        Duration ret = ProcessableUtils.invokeProcessablesAndScheduleEarliestDuration(time,
//                incomingRequestManager);
//        
//        return ret;
//    }
}
