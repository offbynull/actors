package com.offbynull.peernetic.debug.testnetwork.messages;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.debug.testnetwork.LineFactory;
import org.apache.commons.lang3.Validate;

public class StartHub<A> {
    private final EndpointScheduler endpointScheduler;
    private final LineFactory lineFactory;
    private final Endpoint selfEndpoint;

    public StartHub(EndpointScheduler endpointScheduler, LineFactory lineFactory, Endpoint selfEndpoint) {
        Validate.notNull(endpointScheduler);
        Validate.notNull(lineFactory);
        Validate.notNull(selfEndpoint);
        this.endpointScheduler = endpointScheduler;
        this.lineFactory = lineFactory;
        this.selfEndpoint = selfEndpoint;
    }

    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }

    public LineFactory getLineFactory() {
        return lineFactory;
    }
    
    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }
}
