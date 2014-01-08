package com.offbynull.peernetic.actor.network.transports.shared;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Map;

public final class ResponseActor extends Actor {

    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        return new ActorStartSettings();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        Incoming request;
        while ((request = pullQueue.pull()) != null) {
            Object content = request.getContent();
            pushQueue.push(request.getSource(), content);
            
            if (content.equals(50L)) {
                return -1;
            }
        }
        
        return Long.MAX_VALUE;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }
    
}
