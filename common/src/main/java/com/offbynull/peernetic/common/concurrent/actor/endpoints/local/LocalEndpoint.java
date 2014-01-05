package com.offbynull.peernetic.common.concurrent.actor.endpoints.local;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Endpoint;
import org.apache.commons.lang3.Validate;

public final class LocalEndpoint implements Endpoint {
    private ActorQueue actorQueue;

    public LocalEndpoint(ActorQueue actorQueue) {
        Validate.notNull(actorQueue);
        this.actorQueue = actorQueue;
    }

    ActorQueueWriter getActorQueueWriter() {
        return actorQueue.getWriter();
    }
    
}
