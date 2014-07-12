package com.offbynull.peernetic.actor;

public interface EndpointIdentifier<T> {
    T identify(Endpoint endpoint);
}
