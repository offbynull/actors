package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.common.Processable;
import com.offbynull.peernetic.common.message.NonceAccessor;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

public final class IncomingRequestRouter<A, N> implements Processable {
    
    private final MultiMap<Class<?>, Actor> routerMap;
    private final IncomingRequestManager<A, N> incomingRequestManager;
    private final EndpointDirectory<A> endpointDirectory;

    public IncomingRequestRouter(Endpoint selfEndpoint, NonceAccessor<N> nonceAccessor, EndpointDirectory<A> endpointDirectory) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceAccessor);
        Validate.notNull(endpointDirectory);

        routerMap = MultiValueMap.multiValueMap(new HashMap<Class<?>, Set>(), HashSet.class);
        incomingRequestManager = new IncomingRequestManager<>(selfEndpoint, nonceAccessor);
        this.endpointDirectory = endpointDirectory;
    }

    public void route(Instant time, Endpoint source, Object request) throws Exception {
        Validate.notNull(time);
        Validate.notNull(source);
        Validate.notNull(request);

        boolean shouldHandler = incomingRequestManager.testRequestMessage(time, request);
        if (!shouldHandler) {
            return;
        }
        
        Collection<Actor> actors = (Collection<Actor>) routerMap.get(request.getClass());
        if (actors == null) {
            return;
        }
        
        for (Actor actor : actors) {
            actor.onStep(time, source, request);
        }
    }
    
    @Override
    public Duration process(Instant time) {
        Duration duration = incomingRequestManager.process(time);
        return duration;
    }

    public IncomingRequestRouterController<A, N> getController(Actor actor) {
        Validate.notNull(actor);
        return new IncomingRequestRouterController<>(this, actor);
    }
    
    public static final class IncomingRequestRouterController<A, N> {
        private IncomingRequestRouter<A, N> router;
        private Actor actor;

        private IncomingRequestRouterController(IncomingRequestRouter<A, N> router, Actor actor) {
            Validate.notNull(router);
            Validate.notNull(actor);
            this.router = router;
            this.actor = actor;
        }

        public void registerType(Class<?> type) {
            Validate.notNull(type);
            router.routerMap.put(type, actor);
        }

        public void unregisterType(Class<?> type) {
            Validate.notNull(type);
            router.routerMap.removeMapping(type, actor);
        }
        
        public void sendResponse(Instant time, A destination, Object request, Object response) {
            Validate.notNull(time);
            Validate.notNull(destination);
            Validate.notNull(request);
            Validate.notNull(response);
            Endpoint sender = router.endpointDirectory.lookup(destination);
            router.incomingRequestManager.sendResponseAndTrack(time, request, response, sender);
        }
        
        public void sendResponse(Instant time, Endpoint destination, Object request, Object response) {
            Validate.notNull(time);
            Validate.notNull(destination);
            Validate.notNull(request);
            Validate.notNull(response);
            router.incomingRequestManager.sendResponseAndTrack(time, request, response, destination);
        }
    }
}
