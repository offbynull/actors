package com.offbynull.peernetic.demo.messages.internal;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start<A> {

    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    private final UnmodifiableSet<A> bootstrapAddresses;
    private final Endpoint selfEndpoint;

    public Start(EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier, EndpointScheduler endpointScheduler,
            Set<A> bootstrapAddresses, Endpoint selfEndpoint) {
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        Validate.notNull(bootstrapAddresses);
        Validate.notNull(selfEndpoint);
        Validate.isTrue(!bootstrapAddresses.isEmpty(), "Must have atleast 1 bootstrap address");
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.bootstrapAddresses = (UnmodifiableSet<A>) UnmodifiableSet.unmodifiableSet(new LinkedHashSet<A>(bootstrapAddresses));
        this.selfEndpoint = selfEndpoint;
    }

    public UnmodifiableSet<A> getBootstrapAddresses() {
        return bootstrapAddresses;
    }

    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }

    public EndpointDirectory<A> getEndpointDirectory() {
        return endpointDirectory;
    }

    public EndpointIdentifier<A> getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }
    
}
