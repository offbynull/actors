package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.Mode.FOLLOWER;
import static com.offbynull.peernetic.examples.raft.Mode.LEADER;
import com.offbynull.peernetic.examples.raft.internalmessages.Kill;
import com.offbynull.peernetic.examples.raft.internalmessages.StartServer;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.PositionUtils;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveNode;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.mutable.MutableObject;

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

        double radius = Math.log(nodeLinks.size() * 100) * 50.0;
        double percentage = hashToPercentage(selfLink);
        Point graphPoint = PositionUtils.pointOnCircle(radius, percentage);
        ctx.addOutgoingMessage(logAddress, debug("Starting server"));
        ctx.addOutgoingMessage(graphAddress, new AddNode(selfLink));
        ctx.addOutgoingMessage(graphAddress, new MoveNode(selfLink, graphPoint.getX(), graphPoint.getY()));
        
        try {
            MutableObject<Mode> newMode = new MutableObject<>();
            CoroutineRunner coroutineRunner;

            // Initialize to follower mode            
            coroutineRunner = new CoroutineRunner(innerCnt -> {
                Mode ret = new FollowerSubcoroutine(state).run(innerCnt);
                newMode.setValue(ret);
            });
            coroutineRunner.setContext(ctx);

            while (true) {
                Object msg = ctx.getIncomingMessage();
                boolean isFromSelf = ctx.getSource().equals(ctx.getSelf());
                
                // Is a kill msg? If so, kill.
                if (isFromSelf && msg instanceof Kill) {
                    throw new RuntimeException("Kill message encountered");
                }
                
                // Run subcoroutine... if we didn't get a signal to switch modes, wait for next message and process that msg
                if (coroutineRunner.execute()) {
                    cnt.suspend();
                    continue;
                }
                
                // Otherwise, switch to new mode and prime with current message
                switch (newMode.getValue()) {
                    case FOLLOWER:
                        coroutineRunner = new CoroutineRunner(innerCnt -> {
                            Mode ret = new FollowerSubcoroutine(state).run(innerCnt);
                            newMode.setValue(ret);
                        });
                        coroutineRunner.setContext(ctx);
                        break;
                    case CANDIDATE:
                        coroutineRunner = new CoroutineRunner(innerCnt -> {
                            Mode ret = new CandidateSubcoroutine(state).run(innerCnt);
                            newMode.setValue(ret);
                        });
                        coroutineRunner.setContext(ctx);
                        break;
                    case LEADER:
                        coroutineRunner = new CoroutineRunner(innerCnt -> {
                            Mode ret = new LeaderSubcoroutine(state).run(innerCnt);
                            newMode.setValue(ret);
                        });
                        coroutineRunner.setContext(ctx);
                        break;
                    default:
                        throw new IllegalStateException();
                }
                newMode.setValue(null); // reset newMode
            }
        } finally {
            ctx.addOutgoingMessage(graphAddress, new RemoveNode(selfLink));
        }
    }
    
    private double hashToPercentage(String val) throws Exception {
        byte[] data = DigestUtils.md5(val);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dais = new DataInputStream(bais);
        
        int shortVal = dais.readUnsignedShort();
        return (double) shortVal / (double) 0xFFFF;
    }
}
