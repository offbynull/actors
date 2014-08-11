package com.offbynull.peernetic.demos.chord.messages.internal;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.Id;
import org.apache.commons.lang3.Validate;

public final class Start<A> {

    private final EndpointDirectory<A> endpointDirectory;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final Id selfId;
    private final A bootstrapAddress;

    public Start(EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier, EndpointScheduler endpointScheduler,
            Endpoint selfEndpoint, Id selfId, A bootstrapAddress) {
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(selfId);
//        Validate.notNull(bootstrapAddress); // may be null
        
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
        this.selfId = selfId;
        this.bootstrapAddress = bootstrapAddress;
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

    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }

    public Id getSelfId() {
        return selfId;
    }

    public A getBootstrapAddress() {
        return bootstrapAddress;
    }
    
}
