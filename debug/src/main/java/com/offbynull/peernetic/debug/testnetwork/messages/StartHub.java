package com.offbynull.peernetic.debug.testnetwork.messages;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.debug.testnetwork.Line;
import com.offbynull.peernetic.network.Serializer;
import org.apache.commons.lang3.Validate;

public class StartHub<A> {
    private final EndpointScheduler endpointScheduler;
    private final Line<A> line;
    private final Serializer serializer;
    private final Endpoint selfEndpoint;

    public StartHub(EndpointScheduler endpointScheduler, Line<A> line, Serializer serializer, Endpoint selfEndpoint) {
        Validate.notNull(endpointScheduler);
        Validate.notNull(line);
        Validate.notNull(serializer);
        Validate.notNull(selfEndpoint);
        this.endpointScheduler = endpointScheduler;
        this.line = line;
        this.serializer = serializer;
        this.selfEndpoint = selfEndpoint;
    }

    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }

    public Line<A> getLine() {
        return line;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    
    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }
}
