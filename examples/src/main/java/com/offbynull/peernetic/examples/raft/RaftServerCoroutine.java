package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.raft.externalmessages.HeartbeatRequest;
import com.offbynull.peernetic.examples.raft.internalmessages.ElectionTimeout;
import com.offbynull.peernetic.examples.raft.internalmessages.Start;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class RaftServerCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Start start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerPrefix();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        UnmodifiableSet<String> nodeLinks = start.getNodeLinks();
        AddressTransformer addressTransformer = start.getAddressTransformer();
        long seed = start.getSeed();

        Address self = ctx.getSelf();
        
        Random random = new Random(seed);
        String selfLink = addressTransformer.selfAddressToLinkId(self);
        Set<String> otherNodeLinks = new HashSet<>(nodeLinks);
        Validate.isTrue(otherNodeLinks.contains(selfLink));
        otherNodeLinks.remove(selfLink);
        

        int term = 0;
        State state = State.FOLLOWER;
        
        top:
        while (true) {
            switch (state) {
                case FOLLOWER: {
                    ElectionTimeout timeoutObj = new ElectionTimeout();
                    Address timeoutAddress = timerAddress.appendSuffix("" + randBetween(random, 150, 300));
                    ctx.addOutgoingMessage(timeoutAddress, timeoutObj);

                    while (true) {
                        cnt.suspend();
                        Object incomingMsg = ctx.getIncomingMessage();
                        if (incomingMsg == timeoutObj) {
                            // The timeout has been hit without a heartbeat coming in. Switch to candidate mode.
                            state = State.CANDIDATE;
                            continue top;
                        } else if (incomingMsg instanceof HeartbeatRequest) {
                            // A heartbeat message as come in, reset.
                            continue top;
                        }
                    }
                }
                case CANDIDATE: {
                    break;
                }
                case LEADER: {
                    break;
                }
            }
        }
    }
    
    private int randBetween(Random random, int start, int end) {
        return random.nextInt(end - start) + start;
    }
    
    private enum State {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }
    
}
