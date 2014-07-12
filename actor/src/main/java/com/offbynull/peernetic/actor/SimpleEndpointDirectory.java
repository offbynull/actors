package com.offbynull.peernetic.actor;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;

public final class SimpleEndpointDirectory<T> implements EndpointDirectory<T> {
    private UnmodifiableMap<T, Endpoint> lookupMap;

    public SimpleEndpointDirectory(Map<Endpoint, T> lookupMap) {
        this.lookupMap = (UnmodifiableMap) UnmodifiableMap.unmodifiableMap(new HashMap<>(lookupMap));
    }
    
    @Override
    public Endpoint lookup(T id) {
        return lookupMap.get(id);
    }
    
}
