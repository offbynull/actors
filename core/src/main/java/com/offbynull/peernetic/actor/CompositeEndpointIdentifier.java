package com.offbynull.peernetic.actor;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class CompositeEndpointIdentifier<A> implements EndpointIdentifier<A> {

    private List<EndpointIdentifier<A>> endpointIdentifiers;

    public CompositeEndpointIdentifier(List<EndpointIdentifier<A>> endpointIdentifiers) {
        Validate.noNullElements(endpointIdentifiers);
        this.endpointIdentifiers = new ArrayList<>(endpointIdentifiers);
    }
    
    @Override
    public A identify(Endpoint endpoint) {
        for (EndpointIdentifier<A> endpointIdentifier : endpointIdentifiers) {
            A id = endpointIdentifier.identify(endpoint);
            if (id != null) {
                return id;
            }
        }
        
        return null;
    }
    
}
