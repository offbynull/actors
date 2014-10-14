package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.message.Nonce;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class IncomingRequestState<N> {
    private final IncomingRequestTypeParameters parameters;
    private final Endpoint endpoint;
    private final Nonce<N> nonce;

    public IncomingRequestState(IncomingRequestTypeParameters parameters, Endpoint endpoint, Nonce<N> nonce, Object request) {
        Validate.notNull(parameters);
        Validate.notNull(endpoint);
        Validate.notNull(nonce);
        Validate.notNull(request);
        this.parameters = parameters;
        this.endpoint = endpoint;
        this.nonce = nonce;
    }

    public IncomingRequestTypeParameters getParameters() {
        return parameters;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Nonce<N> getNonce() {
        return nonce;
    }
    
    public Duration getNextDuration() {
        return parameters.getRetainDuration();
    }

    public Object getNextEvent() {
        return new IncomingRequestDiscardEvent<>(nonce);
    }
    
}
