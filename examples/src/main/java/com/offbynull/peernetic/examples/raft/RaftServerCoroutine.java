package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineStepper;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.Mode.FOLLOWER;
import static com.offbynull.peernetic.examples.raft.Mode.LEADER;
import com.offbynull.peernetic.examples.raft.internalmessages.Kill;
import com.offbynull.peernetic.examples.raft.internalmessages.StartServer;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveNode;
import org.apache.commons.collections4.set.UnmodifiableSet;

public final class RaftServerCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        StartServer start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerAddress();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        int minElectionTimeout = start.getMinElectionTimeout();
        int maxElectionTimeout = start.getMaxElectionTimeout();
        UnmodifiableSet<String> nodeLinks = start.getNodeLinks();
        AddressTransformer addressTransformer = start.getAddressTransformer();
        byte[] seed = start.getSeed();

        Address self = ctx.getSelf();
        String selfLink = addressTransformer.selfAddressToLinkId(self);
        
        ServerState state = new ServerState(timerAddress, graphAddress, logAddress, seed, minElectionTimeout, maxElectionTimeout, selfLink,
                nodeLinks, addressTransformer);

        ctx.addOutgoingMessage(logAddress, debug("Starting server"));
        ctx.addOutgoingMessage(graphAddress, new AddNode(selfLink));
        
        try {
            SubcoroutineStepper<Mode> stepper;

            // Initialize to follower mode            
            stepper = new SubcoroutineStepper<>(ctx, new FollowerSubcoroutine(state));

            while (true) {
                Object msg = ctx.getIncomingMessage();
                boolean isFromSelf = ctx.getSource().equals(ctx.getSelf());
                
                // Is a kill msg? If so, kill.
                if (isFromSelf && msg instanceof Kill) {
                    throw new RuntimeException("Kill message encountered");
                }
                
                // Run subcoroutine... if we didn't get a signal to switch modes, wait for next message and process that msg
                if (stepper.step()) {
                    cnt.suspend();
                    continue;
                }
                
                // Otherwise, switch to new mode and prime with current message
                Mode newMode = stepper.getResult();
                switch (newMode) {
                    case FOLLOWER:
                        stepper = new SubcoroutineStepper<>(ctx, new FollowerSubcoroutine(state));
                        break;
                    case CANDIDATE:
                        stepper = new SubcoroutineStepper<>(ctx, new CandidateSubcoroutine(state));
                        break;
                    case LEADER:
                        stepper = new SubcoroutineStepper<>(ctx, new LeaderSubcoroutine(state));
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        } finally {
            ctx.addOutgoingMessage(graphAddress, new RemoveNode(selfLink, true, false));
        }
    }
}
