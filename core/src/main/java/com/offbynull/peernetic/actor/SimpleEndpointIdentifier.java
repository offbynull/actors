package com.offbynull.peernetic.actor;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;

public final class SimpleEndpointIdentifier<T> implements EndpointIdentifier<T> {
    private UnmodifiableMap<Endpoint, T> lookupMap;

    public SimpleEndpointIdentifier(Map<Endpoint, T> lookupMap) {
        Validate.noNullElements(lookupMap.keySet());
        Validate.noNullElements(lookupMap.values());
        this.lookupMap = (UnmodifiableMap) UnmodifiableMap.unmodifiableMap(new HashMap<>(lookupMap));
    }

    @Override
    public T identify(Endpoint endpoint) {
        return lookupMap.get(endpoint);
    }
    
}
