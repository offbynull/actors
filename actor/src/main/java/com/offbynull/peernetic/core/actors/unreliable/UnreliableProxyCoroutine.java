package com.offbynull.peernetic.core.actors.unreliable;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.AddressUtils;
import java.time.Instant;
import java.util.Collection;

public final class UnreliableProxyCoroutine implements Coroutine {

    @Override
    public void run(Continuation cont) throws Exception {
        Context ctx = (Context) cont.getContext();

        String selfAddr = ctx.getSelf();

        StartUnreliableProxy startMsg = ctx.getIncomingMessage();

        Line line = startMsg.getLine();
        String timerPrefix = startMsg.getTimerPrefix();
        String actorPrefix = startMsg.getActorPrefix();

        while (true) {
            cont.suspend();
            String srcAddr = ctx.getSource();
            Object msg = ctx.getIncomingMessage();
            Instant time = ctx.getTime();
            
            if (AddressUtils.isParent(timerPrefix, srcAddr)) {
                // Timer message indicating that a message is suppose to go out now
                TransitMessage tm = (TransitMessage) msg;

                for (DepartMessage dm : line.messageArrive(time, tm)) {
                    String dstAddr = dm.getDestinationAddress();
                    String srcSuffix = dm.getSourceSuffix();
                    Object sendMsg = dm.getMessage();

                    ctx.addOutgoingMessage(srcSuffix, dstAddr, sendMsg);
                }
            } else if (AddressUtils.isParent(actorPrefix, srcAddr)) {
                // Outgoing message
                
                  // Get address to proxy to
                String dstAddr = ctx.getDestination();
                String proxyToAddr = AddressUtils.relativize(selfAddr, dstAddr); // treat suffix for dst of this msg as address to proxy to
                
                  // Get suffix for from address
                String srcSuffix = AddressUtils.relativize(actorPrefix, srcAddr);

                  // Generate transit messages
                DepartMessage departMessage = new DepartMessage(msg, srcSuffix, proxyToAddr);
                for (TransitMessage tm : line.messageDepart(time, departMessage)) {
                    ctx.addOutgoingMessage(timerPrefix + SEPARATOR + tm.getDuration().toMillis(), tm);
                }
            } else {
                // Incoming message
                
                  // Get suffix portion of incoming message's destination address
                String dstAddr = ctx.getDestination();
                String suffix = AddressUtils.relativize(selfAddr, dstAddr);
                
                  // Add source address to suffix of proxy's address
                ctx.addOutgoingMessage(
                        srcAddr,
                        actorPrefix + (suffix != null ? SEPARATOR + suffix : ""),
                        msg);
                
            }
        }
    }
}
