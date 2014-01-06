package com.offbynull.peernetic.actor.tests;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorQueue;
import com.offbynull.peernetic.actor.IncomingRequest;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Map;

public final class ResponseActor extends Actor {
    public ResponseActor() {
        super(true);
    }

    @Override
    protected ActorQueue onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        return new ActorQueue();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue) throws Exception {
        IncomingRequest request;
        while ((request = pullQueue.pullRequest(timestamp)) != null) {
            Object content = request.getContent();
            pushQueue.pushResponse(request, content);
            
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
