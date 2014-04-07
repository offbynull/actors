package com.offbynull.peernetic.actor.tests;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Map;

public final class RequestActor extends Actor {
    private volatile long number;
    private Endpoint friend;

    public void setFriend(Endpoint friend) {
        putInStartupMap("friend", friend);
    }

    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        friend = (Endpoint) initVars.get("friend");
        
        pushQueue.push(friend, number);
        
        return new ActorStartSettings(Long.MAX_VALUE);
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
