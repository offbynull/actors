package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.common.Processable;
import com.offbynull.peernetic.common.ProcessableUtils;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.NonceGenerator;
import com.offbynull.peernetic.common.message.NonceManager;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class OutgoingRequestRouter<A, N> implements Processable {
    
    private final NonceManager<N> nonceManager;
    private final NonceAccessor<N> nonceAccessor;
    private final OutgoingRequestManager<A, N> outgoingRequestManager;
    private final EndpointIdentifier<A> endpointIdentifier;

    public OutgoingRequestRouter(Endpoint selfEndpoint, NonceGenerator<N> nonceGenerator, NonceAccessor<N> nonceAccessor,
            EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceAccessor);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        
        nonceManager = new NonceManager<>();
        outgoingRequestManager = new OutgoingRequestManager(selfEndpoint, nonceGenerator, nonceAccessor, endpointDirectory);
        this.endpointIdentifier = endpointIdentifier;
        this.nonceAccessor = nonceAccessor;
    }

    public void route(Instant time, Endpoint source, Object response) throws Exception {
        Validate.notNull(time);
        Validate.notNull(source);
        Validate.notNull(response);

        boolean needsHandling = outgoingRequestManager.testResponseMessage(time, response);
        if (!needsHandling) {
            return;
        }
        
        Nonce<N> nonce = nonceAccessor.get(response);
        if (nonceManager.isNoncePresent(nonce)) {
            ((Actor) nonceManager.getNonceValue(nonce)).onStep(time, source, response);
        }
        nonceManager.removeNonce(nonce);
    }
    
    @Override
    public Duration process(Instant time) {
        return ProcessableUtils.invokeProcessablesAndScheduleEarliestDuration(time,
                outgoingRequestManager,
                nonceManager);
    }
    
    public OutgoingRequestRouterController<A, N> getController(Actor actor) {
        Validate.notNull(actor);
        return new OutgoingRequestRouterController<>(this, actor);
    }
    
    public static final class OutgoingRequestRouterController<A, N> {
        private OutgoingRequestRouter<A, N> router;
        private Actor actor;

        private OutgoingRequestRouterController(OutgoingRequestRouter<A, N> router, Actor actor) {
            Validate.notNull(router);
            Validate.notNull(actor);
            this.router = router;
            this.actor = actor;
        }

        public void sendRequest(Instant time, A destination, Object request) {
            Validate.notNull(time);
            Validate.notNull(destination);
            Validate.notNull(request);
            Nonce<N> nonce = router.outgoingRequestManager.sendRequestAndTrack(time, request, destination);
            Duration duration = router.outgoingRequestManager.getDefaultRetainDuration();
            router.nonceManager.addNonce(time, duration, nonce, actor);
        }
        
        public void sendRequest(Instant time, Endpoint destination, Object request) {
            Validate.notNull(time);
            Validate.notNull(destination);
            Validate.notNull(request);
            A address = router.endpointIdentifier.identify(destination);
            Nonce<N> nonce = router.outgoingRequestManager.sendRequestAndTrack(time, request, address);
            Duration duration = router.outgoingRequestManager.getDefaultRetainDuration();
            router.nonceManager.addNonce(time, duration, nonce, actor);
        }

        public int getPending() {
            return router.outgoingRequestManager.getPending();
        }
    }
}
