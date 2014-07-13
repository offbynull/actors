package com.offbynull.peernetic.demo.messages.internal;

import com.offbynull.peernetic.actor.Endpoint;
import org.apache.commons.lang3.Validate;

public final class StartSeed<A> {

    private final Endpoint selfEndpoint;

    public StartSeed(Endpoint selfEndpoint) {
        Validate.notNull(selfEndpoint);
        this.selfEndpoint = selfEndpoint;
    }

    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }
    
}
