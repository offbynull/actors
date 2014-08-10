package com.offbynull.peernetic.demos.chord.messages.internal;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceWrapper;
import org.apache.commons.lang3.Validate;

public final class Start<A> {

    private final EndpointDirectory<A> endpointDirectory;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final NonceGenerator<byte[]> nonceGenerator;
    private final NonceWrapper<byte[]> nonceWrapper;

    private final Id selfId;
    private final A bootstrapAddress;

    public Start(EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier, EndpointScheduler endpointScheduler,
            Endpoint selfEndpoint, NonceGenerator<byte[]> nonceGenerator, NonceWrapper<byte[]> nonceWrapper, Id selfId,
            A bootstrapAddress) {
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceWrapper);
        Validate.notNull(selfId);
        Validate.notNull(bootstrapAddress);
        
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
        this.nonceGenerator = nonceGenerator;
        this.nonceWrapper = nonceWrapper;
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

    public NonceGenerator<byte[]> getNonceGenerator() {
        return nonceGenerator;
    }

    public NonceWrapper<byte[]> getNonceWrapper() {
        return nonceWrapper;
    }

    public Id getSelfId() {
        return selfId;
    }

    public A getBootstrapAddress() {
        return bootstrapAddress;
    }
    
}
