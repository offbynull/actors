package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.message.NonceAccessor;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;

final class StartEvent<A, N> {

    private final Endpoint selfEndpoint;
    private final Endpoint userEndpoint;
    private final EndpointScheduler endpointScheduler;
    private final EndpointDirectory<A> endpointDirectory;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final NonceAccessor<N> nonceAccessor;
    private final UnmodifiableMap<Class<?>, TypeParameters> typeParameters;


    public StartEvent(Endpoint selfEndpoint, Endpoint userEndpoint, EndpointScheduler endpointScheduler,
            EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier, NonceAccessor<N> nonceAccessor,
            Map<Class<?>, TypeParameters> typeParameters) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(userEndpoint);
        Validate.notNull(endpointScheduler);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(nonceAccessor);
        Validate.notNull(typeParameters);
        Validate.noNullElements(typeParameters.keySet());
        Validate.noNullElements(typeParameters.values());
        
        this.selfEndpoint = selfEndpoint;
        this.userEndpoint = userEndpoint;
        this.endpointScheduler = endpointScheduler;
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.nonceAccessor = nonceAccessor;
        this.typeParameters = (UnmodifiableMap<Class<?>, TypeParameters>) UnmodifiableMap.unmodifiableMap(typeParameters);
    }

    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }

    public Endpoint getUserEndpoint() {
        return userEndpoint;
    }

    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }

    public EndpointDirectory<A> getEndpointDirectory() {
        return endpointDirectory;
    }

    public EndpointIdentifier<A> getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public NonceAccessor<N> getNonceAccessor() {
        return nonceAccessor;
    }

    public UnmodifiableMap<Class<?>, TypeParameters> getTypeParameters() {
        return typeParameters;
    }
    
}
