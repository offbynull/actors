package com.offbynull.peernetic.actor.tests;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorQueue;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.IncomingResponse;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Map;

public final class RequesterActor extends Actor {
    private volatile long number;
    private Endpoint friend;

    public RequesterActor() {
        super(true);
    }

    public void setFriend(Endpoint friend) {
        putInStartupMap("friend", friend);
    }

    @Override
    protected ActorQueue onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        friend = (Endpoint) initVars.get("friend");
        
        pushQueue.pushRequest(friend, number, 1000L);
        
        return new ActorQueue();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue) throws Exception {
        IncomingResponse response;
        while ((response = pullQueue.pullResponse()) != null) {
            Object content = response.getContent();
            if (content.equals(number)) {
                if (number == 50L) {
                    return -1;
                }
                
                number++;
                pushQueue.pushRequest(friend, number, 10000L);
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
