package com.offbynull.peernetic.core.gateways.direct;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import org.junit.Assert;
import org.junit.Test;

public class DirectGatewayTest {

    @Test
    public void mustBeAbleToCommunicateBetweenActorAndDirectGateway() throws InterruptedException {
        Coroutine echoerActor = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("direct", "ready");
            
            cnt.suspend();
            
            Address sender = ctx.source();
            Object msg = ctx.in();
            ctx.out(sender, msg);
        };

        ActorRunner actorRunner = ActorRunner.create("actors");
        DirectGateway directGateway = DirectGateway.create("direct");

        directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());

        actorRunner.addActor("echoer", echoerActor, new Object());
        Address echoerAddress = Address.fromString("actors:echoer");

        Assert.assertEquals("ready", directGateway.readMessages().get(0).getMessage());
        directGateway.writeMessage(echoerAddress, "echotest");
        Assert.assertEquals("echotest", directGateway.readMessages().get(0).getMessage());

        actorRunner.close();
        directGateway.close();
    }

}
