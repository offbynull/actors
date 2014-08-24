package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.common.Processable;
import com.offbynull.peernetic.common.ProcessableUtils;
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

public final class Router<A, N> implements Processable {
    private NonceAccessor<N> nonceAccessor;
    
    private OutgoingRequestManager<A, N> outgoingRequestManager;
    private IncomingRequestManager<A, N> incomingRequestManager;
    private MultiMap<Class<?>, Actor> typeHandlers;
    private Map<Nonce<N>, Actor> responseNonceHandlers;
    
    public Router(Endpoint selfEndpoint, NonceGenerator<N> nonceGenerator, NonceAccessor<N> nonceAccessor,
            EndpointDirectory<A> endpointDirectory) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceAccessor);
        Validate.notNull(endpointDirectory);
        
        this.nonceAccessor = nonceAccessor;
        
        outgoingRequestManager = new OutgoingRequestManager(selfEndpoint, nonceGenerator, nonceAccessor, endpointDirectory);
        incomingRequestManager = new IncomingRequestManager(selfEndpoint, nonceAccessor);
        typeHandlers = new MultiValueMap<>();
        responseNonceHandlers = new HashMap<>();
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

    public void removeNonce(Nonce<N> nonce) {
        Validate.notNull(nonce);
        responseNonceHandlers.remove(nonce);
    }

    public void removeAllNoncesForActor(Actor actor) {
        Validate.notNull(actor);
        
        Set<Nonce<N>> keysCopy = new HashSet<>(responseNonceHandlers.keySet());
        keysCopy.stream().forEach(n -> responseNonceHandlers.remove(n, actor));
    }

    public void removeActor(Actor actor) {
        Validate.notNull(actor);
        
        removeAllNoncesForActor(actor);
        removeAllTypeHandlersForActor(actor);
    }
    
    public void sendRequest(Actor actor, Instant time, Object request, A dstAddress) {
        Validate.notNull(actor);
        Validate.notNull(time);
        Validate.notNull(request);
        Validate.notNull(dstAddress);
        
        Nonce<N> nonce = outgoingRequestManager.sendRequestAndTrack(time, request, dstAddress);
        responseNonceHandlers.put(nonce, actor);
    }

    public void sendResponse(Instant time, Object request, Object response, Endpoint srcEndpoint) {
        Validate.notNull(time);
        Validate.notNull(request);
        Validate.notNull(response);
        Validate.notNull(srcEndpoint);
        
        incomingRequestManager.sendResponseAndTrack(time, request, response, srcEndpoint);
    }
    
    public void routeMessage(Instant time, Object message, Endpoint srcEndpoint) throws Exception {
        Validate.notNull(time);
        Validate.notNull(message);
        Validate.notNull(srcEndpoint);
        
        Collection<Actor> actors = (Collection<Actor>) typeHandlers.get(message.getClass());
        actors = CollectionUtils.emptyIfNull(actors);
        
        for (Actor actor : actors) {
            actor.onStep(time, srcEndpoint, message);
        }
        
        
        
        if (!nonceAccessor.containsNonceField(message)) {
            return;
        }
        
        Actor actor = responseNonceHandlers.remove(nonceAccessor.get(message));
        
        if (actor == null
                || outgoingRequestManager.isTrackedRequest(time, message)
                || !outgoingRequestManager.testResponseMessage(time, message)
                || incomingRequestManager.testRequestMessage(time, message)) {
            // if not expecting a response for this nonce value
            //   or if message being tested is a request that we originally sent out
            //   or if message being tested is NOT a response to a message we originally sent out
            //   or if message being tested is a request that we've responded to already (if it is call sends out a copy of orig response)
            return;
        }
        
        actor.onStep(time, srcEndpoint, message);
    }

    @Override
    public Duration process(Instant time) {
        Duration ret = ProcessableUtils.invokeProcessablesAndScheduleEarliestDuration(time,
                incomingRequestManager,
                outgoingRequestManager);
        
        outgoingRequestManager.getRemovedNonces().forEach(x -> responseNonceHandlers.remove(x));
        
        return ret;
    }
}
