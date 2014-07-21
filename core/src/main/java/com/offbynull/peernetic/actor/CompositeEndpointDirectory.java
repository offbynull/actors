package com.offbynull.peernetic.actor;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class CompositeEndpointDirectory<A> implements EndpointDirectory<A> {

    private List<EndpointDirectory<A>> endpointDirectories;

    public CompositeEndpointDirectory(List<EndpointDirectory<A>> endpointDirectories) {
        Validate.noNullElements(endpointDirectories);
        this.endpointDirectories = new ArrayList<>(endpointDirectories);
    }
    
    @Override
    public Endpoint lookup(A id) {
        for (EndpointDirectory<A> endpointDirectory : endpointDirectories) {
            Endpoint endpoint = endpointDirectory.lookup(id);
            if (endpoint != null) {
                return endpoint;
            }
        }
        
        return null;
    }
    
}
