package com.offbynull.actors.core.gateways.direct;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.core.actor.ActorRunner;
import com.offbynull.actors.core.actor.Context;
import com.offbynull.actors.core.shuttle.Address;
import org.junit.Assert;
import org.junit.Test;

public class DirectGatewayTest {

    @Test
    public void mustBeAbleToCommunicateBetweenActorAndDirectGateway() throws InterruptedException {
        Coroutine echoerActor = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.ruleSet().allowAll();
            ctx.out("direct", "ready");
            
            cnt.suspend();
            
            Address sender = ctx.source();
            Object msg = ctx.in();
            ctx.out(sender, msg);
        };

        try (ActorRunner actorRunner = ActorRunner.create("actors", 1);
                DirectGateway directGateway = DirectGateway.create("direct")) {            
            directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
            actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());

            actorRunner.addActor("echoer", echoerActor, new Object());
            Address echoerAddress = Address.fromString("actors:echoer");

            Assert.assertEquals("ready", directGateway.readMessages().get(0).getMessage());
            directGateway.writeMessage(echoerAddress, "echotest");
            Assert.assertEquals("echotest", directGateway.readMessages().get(0).getMessage());
        }
    }
}
