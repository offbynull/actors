package com.offbynull.peernetic.core.actors.unreliable;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ProxyHelper;
import com.offbynull.peernetic.core.actor.ProxyHelper.ForwardInformation;
import static com.offbynull.peernetic.core.shuttle.AddressUtils.SEPARATOR;
import com.offbynull.peernetic.core.actor.Context;
import java.time.Instant;

public final class UnreliableProxyCoroutine implements Coroutine {

    @Override
    public void run(Continuation cont) throws Exception {
        Context ctx = (Context) cont.getContext();

        StartUnreliableProxy startMsg = ctx.getIncomingMessage();

        Line line = startMsg.getLine();
        String timerPrefix = startMsg.getTimerPrefix();
        String actorPrefix = startMsg.getActorPrefix();
        
        ProxyHelper proxyHelper = new ProxyHelper(ctx, actorPrefix);

        while (true) {
            cont.suspend();
            Object msg = ctx.getIncomingMessage();
            Instant time = ctx.getTime();
            
            if (proxyHelper.isMessageFrom(timerPrefix)) {
                // Timer message indicating that a message is suppose to go out now
                TransitMessage tm = (TransitMessage) msg;
                ctx.addOutgoingMessage(
                        tm.getSourceId(),
                        tm.getDestinationAddress(),
                        tm.getMessage());
            } else if (proxyHelper.isMessageFromActor()) {
                // Outgoing message
                ForwardInformation forwardInfo = proxyHelper.generateOutboundForwardInformation();
                DepartMessage dm = new DepartMessage(msg,
                        forwardInfo.getProxyFromId(),
                        forwardInfo.getProxyToAddress());
                for (TransitMessage tm : line.processOutgoing(time, dm)) {
                    ctx.addOutgoingMessage(timerPrefix + SEPARATOR + tm.getDuration().toMillis(), tm);
                }
            } else {
                // Incoming message
                ForwardInformation forwardInfo = proxyHelper.generatInboundForwardInformation();
                DepartMessage dm = new DepartMessage(msg,
                        forwardInfo.getProxyFromId(),
                        forwardInfo.getProxyToAddress());
                for (TransitMessage tm : line.processIncoming(time, dm)) {
                    ctx.addOutgoingMessage(timerPrefix + SEPARATOR + tm.getDuration().toMillis(), tm);
                }
            }
        }
    }
}
