/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.actors.unreliable;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.helpers.ProxyHelper;
import com.offbynull.peernetic.core.actor.helpers.ProxyHelper.ForwardInformation;
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
