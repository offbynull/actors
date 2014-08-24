package com.offbynull.peernetic.debug.localgateway;

import com.offbynull.peernetic.FsmActor;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.SimpleEndpointScheduler;
import com.offbynull.peernetic.debug.actornetwork.Hub;
import com.offbynull.peernetic.debug.actornetwork.Line;
import com.offbynull.peernetic.debug.actornetwork.messages.StartHub;
import com.offbynull.peernetic.network.Serializer;
import org.apache.commons.lang3.Validate;

public final class LocalGatewayHub<A> {
    
    private final Hub<A> hub;
    private final Actor hubActor;
    private final ActorRunnable hubActorRunnable;
    private final Endpoint hubEndpoint;
    private final EndpointScheduler endpointScheduler;
    
    public LocalGatewayHub(Line<A> line, Serializer serializer) {
        Validate.notNull(line);
        Validate.notNull(serializer);
        
        hub = new Hub<>();
        hubActor = FsmActor.create(hub, Hub.INITIAL_STATE);
        hubActorRunnable = ActorRunnable.createAndStart(hubActor);
        hubEndpoint = hubActorRunnable.getEndpoint(hubActor);
        endpointScheduler = new SimpleEndpointScheduler();
        
        hubEndpoint.send(NullEndpoint.INSTANCE, new StartHub<>(endpointScheduler, line, serializer, hubEndpoint));
        
//                new SimpleLine<>(0L, Duration.ofMillis(500L), Duration.ofMillis(100L), 0.1, 0.9, 10),
//                new XStreamSerializer(),
    }

    Endpoint getHubEndpoint() {
        return hubEndpoint;
    }
    
}
