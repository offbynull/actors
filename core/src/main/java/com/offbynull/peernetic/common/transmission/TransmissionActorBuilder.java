package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.message.NonceAccessor;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class TransmissionActorBuilder<A, N> {
    private Endpoint userEndpoint;
    private EndpointScheduler endpointScheduler;
    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private NonceAccessor<N> nonceAccessor;
    private Map<Class<?>, IncomingRequestTypeParameters> incomingRequestTypeParametersMapping;
    private Map<Class<?>, IncomingResponseTypeParameters> incomingResponseTypeParametersMapping;
    private Map<Class<?>, OutgoingRequestTypeParameters> outgoingRequestTypeParametersMapping;
    private Map<Class<?>, OutgoingResponseTypeParameters> outgoingResponseTypeParametersMapping;

    public TransmissionActorBuilder(Endpoint userEndpoint, EndpointScheduler endpointScheduler, EndpointDirectory<A> endpointDirectory,
            EndpointIdentifier<A> endpointIdentifier, NonceAccessor<N> nonceAccessor) {
        Validate.notNull(userEndpoint);
        Validate.notNull(endpointScheduler);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(nonceAccessor);

        this.userEndpoint = userEndpoint;
        this.endpointScheduler = endpointScheduler;
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.nonceAccessor = nonceAccessor;
        incomingRequestTypeParametersMapping = new HashMap<>();
        incomingResponseTypeParametersMapping = new HashMap<>();
        outgoingRequestTypeParametersMapping = new HashMap<>();
        outgoingResponseTypeParametersMapping = new HashMap<>();
    }
    
    public TransmissionActorBuilder<A, N> addIncomingRequestType(Class<?> type, Duration retainDuration) {
        Validate.notNull(type);
        Validate.notNull(retainDuration);
        Validate.isTrue(retainDuration.compareTo(Duration.ZERO) > 0);
        Validate.isTrue(nonceAccessor.containsNonceField(type));
        
        if (incomingRequestTypeParametersMapping.putIfAbsent(type, new IncomingRequestTypeParameters(retainDuration)) != null) {
            throw new IllegalArgumentException("Already set for type.");
        }
        
        return this;
    }
    
    public TransmissionActorBuilder<A, N> addIncomingResponseType(Class<?> type, Duration retainDuration) {
        Validate.notNull(type);
        Validate.notNull(retainDuration);
        Validate.isTrue(retainDuration.compareTo(Duration.ZERO) > 0);
        Validate.isTrue(nonceAccessor.containsNonceField(type));
        
        if (incomingResponseTypeParametersMapping.putIfAbsent(type, new IncomingResponseTypeParameters(retainDuration)) != null) {
            throw new IllegalArgumentException("Already set for type.");
        }
        
        return this;
    }
    
    public TransmissionActorBuilder<A, N> addOutgoingRequestType(Class<?> type, Duration resendDuration, Duration responseDuration,
            int maxSendCount) {
        Validate.notNull(type);
        Validate.notNull(resendDuration);
        Validate.notNull(responseDuration);
        Validate.isTrue(!resendDuration.isNegative());
        Validate.isTrue(!responseDuration.isNegative());
        Validate.isTrue(maxSendCount > 0);
        Validate.isTrue(resendDuration.multipliedBy(maxSendCount).compareTo(responseDuration) <= 0);
        Validate.isTrue(nonceAccessor.containsNonceField(type));

        
        if (outgoingRequestTypeParametersMapping.putIfAbsent(type,
                new OutgoingRequestTypeParameters(resendDuration, responseDuration, maxSendCount)) != null) {
            throw new IllegalArgumentException("Already set for type.");
        }
        
        return this;
    }

    public TransmissionActorBuilder<A, N> addOutgoingResponseType(Class<?> type, Duration retainDuration) {
        Validate.notNull(type);
        Validate.notNull(retainDuration);
        Validate.isTrue(retainDuration.compareTo(Duration.ZERO) > 0);
        Validate.isTrue(nonceAccessor.containsNonceField(type));
        
        if (outgoingResponseTypeParametersMapping.putIfAbsent(type, new OutgoingResponseTypeParameters(retainDuration)) != null) {
            throw new IllegalArgumentException("Already set for type.");
        }
        
        return this;
    }
    
    public JavaflowActor buildActor() {
        TransmissionTask<A, N> task = new TransmissionTask<>();
        JavaflowActor actor = new JavaflowActor(task);
        return actor;
    }

    public Object buildStartMessage(Endpoint selfEndpoint) {
        Set<Class<?>> types = new HashSet<>();
        
        types.addAll(incomingRequestTypeParametersMapping.keySet());
        types.addAll(incomingResponseTypeParametersMapping.keySet());
        types.addAll(outgoingRequestTypeParametersMapping.keySet());
        types.addAll(outgoingResponseTypeParametersMapping.keySet());
        
        Map<Class<?>, TypeParameters> typeParameters = new HashMap<>();
        types.forEach(x -> typeParameters.put(x, new TypeParameters(
                incomingRequestTypeParametersMapping.get(x),
                incomingResponseTypeParametersMapping.get(x),
                outgoingRequestTypeParametersMapping.get(x),
                outgoingResponseTypeParametersMapping.get(x))
        ));
        
        return new StartEvent<>(selfEndpoint, userEndpoint, endpointScheduler, endpointDirectory,
                endpointIdentifier, nonceAccessor, typeParameters);
    }
}
