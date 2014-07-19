package com.offbynull.peernetic.demo.messages.internal;

import com.offbynull.peernetic.actor.Endpoint;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start<A> {

    private final UnmodifiableSet<A> bootstrapAddresses;
    private final Endpoint selfEndpoint;

    public Start(Set<A> bootstrapAddresses, Endpoint selfEndpoint) {
        Validate.notNull(bootstrapAddresses);
        Validate.notNull(selfEndpoint);
        Validate.isTrue(!bootstrapAddresses.isEmpty(), "Must have atleast 1 bootstrap address");
        this.bootstrapAddresses = (UnmodifiableSet<A>) UnmodifiableSet.unmodifiableSet(new LinkedHashSet<A>(bootstrapAddresses));
        this.selfEndpoint = selfEndpoint;
    }

    public UnmodifiableSet<A> getBootstrapAddresses() {
        return bootstrapAddresses;
    }

    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }
    
}
