package com.offbynull.peernetic.actor;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;

public final class SimpleEndpointIdentifier<T> implements EndpointIdentifier<T> {
    private UnmodifiableMap<Endpoint, T> lookupMap;

    public SimpleEndpointIdentifier(Map<Endpoint, T> lookupMap) {
        this.lookupMap = (UnmodifiableMap) UnmodifiableMap.unmodifiableMap(new HashMap<>(lookupMap));
    }

    @Override
    public T identify(Endpoint endpoint) {
        return lookupMap.get(endpoint);
    }
    
}
