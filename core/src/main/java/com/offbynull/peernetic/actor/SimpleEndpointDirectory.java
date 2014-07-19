package com.offbynull.peernetic.actor;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;

public final class SimpleEndpointDirectory<T> implements EndpointDirectory<T> {
    private UnmodifiableMap<T, Endpoint> lookupMap;

    public SimpleEndpointDirectory(Map<T, Endpoint> lookupMap) {
        Validate.noNullElements(lookupMap.keySet());
        Validate.noNullElements(lookupMap.values());
        this.lookupMap = (UnmodifiableMap) UnmodifiableMap.unmodifiableMap(new HashMap<>(lookupMap));
    }
    
    @Override
    public Endpoint lookup(T id) {
        return lookupMap.get(id);
    }
    
}
