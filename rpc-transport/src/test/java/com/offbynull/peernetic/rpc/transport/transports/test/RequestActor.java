package com.offbynull.peernetic.rpc.transport.transports.test;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorQueue;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.Outgoing;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Map;

public final class RequestActor extends Actor {
    private volatile long number;
    private volatile Endpoint friend;

    public void beginRequests(Endpoint self, Endpoint friend) {
        this.friend = friend;
        self.push(friend, new Outgoing(0L, self));
    }

    @Override
    protected ActorQueue onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        return new ActorQueue();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Object content = incoming.getContent();
            if (content.equals(number)) {
                if (number == 50L) {
                    return -1;
                }
                
                number++;
                pushQueue.push(friend, number);
            }
        }
        
        return Long.MAX_VALUE;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }

    public long getNumber() {
        return number;
    }
    
}
