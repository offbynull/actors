package com.offbynull.actors;

import com.offbynull.actors.gateways.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ActorSystemTest {

    @Test(timeout = 2000L)
    public void mustCreateAndCommunicateActorsAndGateways() throws Exception {
        Coroutine echoer = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            cnt.suspend();

            ctx.out("actor:sender", ctx.in());
        };

        Coroutine sender = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            // Send a message to echoer
            ctx.out("actor:echoer", "hi");
            cnt.suspend();

            // Forward the response we got to the direct gateway
            String response = ctx.in();
            ctx.out("direct:test", response);
        };

        try (ActorSystem actorSystem = ActorSystem.createDefault()) {
            actorSystem.getDirectGateway().listen("direct:test");
            
            actorSystem.getActorGateway().addActor("echoer", echoer, new Object());
            actorSystem.getActorGateway().addActor("sender", sender, new Object());
            
            String output = actorSystem.getDirectGateway().readMessagePayloadOnly("direct:test");
            
            assertEquals("hi", output);
        }
    }
    
}
