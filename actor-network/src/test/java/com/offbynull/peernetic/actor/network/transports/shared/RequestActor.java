package com.offbynull.peernetic.actor.network.transports.shared;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorQueue;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.Outgoing;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.NotifyManager;
import java.util.Map;

public final class RequestActor extends Actor {
    private volatile long number;
    private volatile Endpoint friend;
    
    private NotifyManager notifyManager;

    public void beginRequests(Endpoint self, Endpoint friend) {
        this.friend = friend;
        self.push(friend, new Outgoing(0L, self));
    }

    @Override
    protected ActorQueue onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        notifyManager = new NotifyManager();
        return new ActorQueue();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Object content = incoming.getContent();
            if (content.equals(number)) {
                if (number == 5L) {
                    return -1;
                }
                
                notifyManager.reset(timestamp + 500L);
            }
        }
        
        if (notifyManager.process(timestamp)) {
            number++;
            pushQueue.push(friend, number);
            notifyManager.reset(Long.MAX_VALUE);
        }
        
        return notifyManager.getNextTimeoutTimestamp();
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }

    public long getNumber() {
        return number;
    }
    
}
